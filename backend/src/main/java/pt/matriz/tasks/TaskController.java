package pt.matriz.tasks;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import pt.matriz.projects.ProjectController.Project;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final JdbcTemplate db;
    public TaskController(JdbcTemplate db) { this.db = db; }
    public record Task(long id, long projectId, String title, String description, String status,
                       String priority, Instant createdAt, List<Long> responsibleUserIds) {}
    public record Comment(long id, long taskId, long authorId, String content, Instant createdAt) {}
    public record History(long id, long taskId, long changedByUserId, String field, Object previousValue,
                          Object newValue, Instant changedAt) {}
    public record Counts(long TODO, long IN_PROGRESS, long DONE, long total) {}
    public record ListResult<T>(List<T> items, int totalItems) {}
    private record Access(long projectId, String role) {}

    private Long user(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        var ids = db.query("SELECT id FROM app_users WHERE lower(email)=lower(?)",
                (r,n) -> r.getLong(1), auth.getName());
        return ids.isEmpty() ? null : ids.get(0);
    }
    private Access project(long id, long uid) {
        var roles = db.query("SELECT role FROM project_memberships WHERE project_id=? AND user_id=?",
                (r,n)->r.getString(1), id,uid);
        return roles.isEmpty()?null:new Access(id,roles.get(0));
    }
    private Access taskAccess(long id, long uid) {
        var access = db.query("SELECT t.project_id, pm.role FROM tasks t JOIN project_memberships pm ON pm.project_id=t.project_id WHERE t.id=? AND pm.user_id=?",
                (r,n)->new Access(r.getLong(1),r.getString(2)),id,uid);
        return access.isEmpty()?null:access.get(0);
    }
    private boolean manager(Access a) { return a != null && "gestor".equals(a.role()); }
    private boolean editor(Access a,long taskId,long uid) {
        return manager(a) || (a!=null && db.queryForObject("SELECT COUNT(*) FROM task_assignments WHERE task_id=? AND user_id=?",
                Integer.class,taskId,uid)>0);
    }
    private ResponseEntity<?> missing() { return pt.matriz.ApiProblems.response(404); }
    private ResponseEntity<?> denied() { return pt.matriz.ApiProblems.response(403); }
    private ResponseEntity<?> invalid() { return ResponseEntity.badRequest().body(Map.of("error","Invalid request")); }
    private boolean text(String s,int min,int max) {
        if(s==null)return false;
        String value=s.strip();
        int length=value.codePointCount(0,value.length());
        return length>=min && length<=max;
    }
    private boolean title(String s) {
        if(s==null)return false;
        String value=s.strip();
        int length=value.codePointCount(0,value.length());
        return length>=3 && length<=150;
    }
    private boolean description(String s) { return s==null || text(s,1,2000); }
    private String norm(String s) { return s==null?null:s.strip(); }
    private boolean status(String s) { return s!=null && Set.of("TODO","IN_PROGRESS","DONE").contains(s); }
    private boolean priority(String s) { return s!=null && Set.of("LOW","MEDIUM","HIGH").contains(s); }
    private List<Long> assignees(long taskId) { return db.query("SELECT user_id FROM task_assignments WHERE task_id=? ORDER BY user_id",
            (r,n)->r.getLong(1),taskId); }
    private Task task(long id) {
        var items=db.query("SELECT id,project_id,title,description,status,priority,created_at FROM tasks WHERE id=?",
            (r,n)->new Task(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),
                r.getTimestamp(7).toInstant(),assignees(r.getLong(1))),id);
        return items.isEmpty()?null:items.get(0);
    }
    private List<Task> tasks(long projectId,String state,String prio) {
        String sql="SELECT id FROM tasks WHERE project_id=?";
        List<Object> args=new ArrayList<>();args.add(projectId);
        if(state!=null){sql+=" AND status=?";args.add(state);}
        if(prio!=null){sql+=" AND priority=?";args.add(prio);}
        return db.query(sql+" ORDER BY created_at DESC,id DESC",
            (r,n)->task(r.getLong(1)),args.toArray());
    }
    private Counts counts(long projectId,String prio) {
        String sql="SELECT status,COUNT(*) AS total FROM tasks WHERE project_id=?"
            +(prio==null ? "" : " AND priority=?")+" GROUP BY status";
        Object[] args=prio==null ? new Object[]{projectId} : new Object[]{projectId,prio};
        var vals=db.query(sql,(r,n)->Map.entry(r.getString(1),r.getLong(2)),args);
        Map<String,Long> m=new HashMap<>(); vals.forEach(v->m.put(v.getKey(),v.getValue()));
        return new Counts(m.getOrDefault("TODO",0L),m.getOrDefault("IN_PROGRESS",0L),m.getOrDefault("DONE",0L),m.values().stream().mapToLong(Long::longValue).sum());
    }
    private boolean eligible(long projectId,List<Long> ids) {
        if(ids==null || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size()!=ids.size()) return false;
        if(ids.isEmpty())return true;
        Integer n=db.queryForObject("SELECT COUNT(*) FROM project_memberships WHERE project_id=? AND user_id IN ("+
            String.join(",",Collections.nCopies(ids.size(),"?"))+")",Integer.class,
            concat(projectId,ids));
        return n!=null && n==ids.size();
    }
    private Object[] concat(long projectId,List<Long> ids) {
        Object[] args=new Object[ids.size()+1];args[0]=projectId;
        for(int i=0;i<ids.size();i++)args[i+1]=ids.get(i);
        return args;
    }
    private void setAssignees(long id,List<Long> ids) {
        db.update("DELETE FROM task_assignments WHERE task_id=?",id);
        for(Long uid:ids) db.update("INSERT INTO task_assignments(task_id,user_id) VALUES (?,?)",id,uid);
    }
    private void history(long id,long actor,String field,String before,String after) {
        if(!before.equals(after))db.update("INSERT INTO task_history(task_id,actor_id,field,previous_value,new_value) VALUES (?,?,?,?,?)",
            id,actor,field,before,after);
    }
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> list(@PathVariable long projectId,@RequestParam(required=false) String status,
                                   @RequestParam(required=false) String priority,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        if(project(projectId,uid)==null)return missing();
        if(status!=null&&!status(status) || priority!=null&&!priority(priority))return invalid();
        List<Task> items=tasks(projectId,status,priority);
        return ResponseEntity.ok(Map.of("items",items,"totalItems",items.size(),"counts",counts(projectId,priority)));
    }
    @PostMapping("/projects/{projectId}/tasks") @Transactional
    public ResponseEntity<?> create(@PathVariable long projectId,@RequestBody Map<String,Object> body,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        Access a=project(projectId,uid);if(a==null)return missing();
        if(body==null || body.keySet().stream().anyMatch(k -> !Set.of("title", "description", "responsibleUserIds").contains(k)))return invalid();
        if(!(body.get("title") instanceof String title) || !title(title)
                || (body.containsKey("description") && body.get("description")!=null && (!(body.get("description") instanceof String d) || !description(d)))
                )return pt.matriz.ApiProblems.validation(body.get("title") instanceof String t && title(t)
                    ? "description" : "title", "Invalid title or description.");
        List<Long> ids=ids(body.getOrDefault("responsibleUserIds",List.of()));if(ids==null)return pt.matriz.ApiProblems.validation("responsibleUserIds", "Invalid responsible user IDs.");
        if(!ids.isEmpty()&&!manager(a))return denied();
        if(!eligible(projectId,ids))return pt.matriz.ApiProblems.validation("responsibleUserIds", "Invalid responsible users.");
        Long id=db.queryForObject("INSERT INTO tasks(project_id,title,description) VALUES (?,?,?) RETURNING id",Long.class,
            projectId,title.strip(),norm((String)body.get("description")));
        setAssignees(id,ids);
        return ResponseEntity.created(URI.create("/api/v1/tasks/"+id)).body(task(id));
    }
    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> get(@PathVariable long id,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        return taskAccess(id,uid)==null?missing():ResponseEntity.ok(task(id));
    }
    @PatchMapping("/tasks/{id}") @Transactional
    public ResponseEntity<?> update(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        Access a=taskAccess(id,uid);if(a==null)return missing();if(!editor(a,id,uid))return denied();
        if(body==null||body.isEmpty()||body.keySet().stream().anyMatch(k->!Set.of("title","description").contains(k)))return invalid();
        if(body.containsKey("title") && (!(body.get("title") instanceof String s)||!title(s)))return pt.matriz.ApiProblems.validation("title", "Title must contain 3 to 150 characters.");
        if(body.containsKey("description")) {Object v=body.get("description");if(v!=null&&(!(v instanceof String s)||!description(s)))return pt.matriz.ApiProblems.validation("description", "Invalid description.");}
        // Update supplied fields only; concurrent PATCHes must not restore omitted values.
        db.update("UPDATE tasks SET title=CASE WHEN ? THEN ? ELSE title END, description=CASE WHEN ? THEN ? ELSE description END WHERE id=?",
            body.containsKey("title"), body.containsKey("title") ? ((String)body.get("title")).strip() : null,
            body.containsKey("description"), norm((String)body.get("description")), id);
        return ResponseEntity.ok(task(id));
    }
    @DeleteMapping("/tasks/{id}") @Transactional
    public ResponseEntity<?> delete(@PathVariable long id,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        Access a=taskAccess(id,uid);if(a==null)return missing();if(!manager(a))return denied();
        db.update("DELETE FROM tasks WHERE id=?",id);return ResponseEntity.noContent().build();
    }
    @PatchMapping("/tasks/{id}/status") @Transactional
    public ResponseEntity<?> changeStatus(@PathVariable long id,@RequestBody Map<String,String> body,Authentication auth) {
        return changeField(id,body,"status",auth);
    }
    @PatchMapping("/tasks/{id}/priority") @Transactional
    public ResponseEntity<?> changePriority(@PathVariable long id,@RequestBody Map<String,String> body,Authentication auth) {
        return changeField(id,body,"priority",auth);
    }
    private ResponseEntity<?> changeField(long id,Map<String,String> body,String field,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        Access a=taskAccess(id,uid);if(a==null)return missing();if(!editor(a,id,uid))return denied();
        if(body==null||body.size()!=1||!body.containsKey(field))return invalid();
        String value=body.get(field);
        if(!(field.equals("status")?status(value):priority(value)))return pt.matriz.ApiProblems.validation(field, "Invalid " + field + ".");
        Task old=task(id);String before=field.equals("status")?old.status():old.priority();
        db.update("UPDATE tasks SET "+field+"=? WHERE id=?",value,id);
        history(id,uid,field,before,value);return ResponseEntity.ok(task(id));
    }
    @GetMapping("/projects/{projectId}/eligible-users")
    public ResponseEntity<?> eligibleUsers(@PathVariable long projectId,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        if(project(projectId,uid)==null)return missing();
        var items=db.query("SELECT u.id,u.name,u.email,m.role FROM app_users u JOIN project_memberships m ON m.user_id=u.id WHERE m.project_id=? ORDER BY lower(u.name),u.id",
            (r,n)->Map.of("user", Map.of("id",r.getLong(1),"name",r.getString(2),"email",r.getString(3)),
                "projectRole",r.getString(4).toUpperCase(Locale.ROOT)),projectId);
        return ResponseEntity.ok(Map.of("items",items,"totalItems",items.size()));
    }
    private List<Long> ids(Object input) {
        if(!(input instanceof List<?> list))return null;
        List<Long> out=new ArrayList<>();
        for(Object o:list){
            if(!(o instanceof Number n))return null;
            try {
                long id=new java.math.BigDecimal(n.toString()).longValueExact();
                if(id<=0)return null;
                out.add(id);
            } catch(ArithmeticException | NumberFormatException e) { return null; }
        }
        return out;
    }
    @PutMapping("/tasks/{id}/responsibles") @Transactional
    public ResponseEntity<?> responsibles(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        Access a=taskAccess(id,uid);if(a==null)return missing();if(!manager(a))return denied();
        if(body==null||body.size()!=1||!body.containsKey("responsibleUserIds"))return invalid();
        List<Long> ids=ids(body.get("responsibleUserIds"));if(ids==null)return pt.matriz.ApiProblems.validation("responsibleUserIds", "Invalid responsible user IDs.");
        if(!eligible(a.projectId(),ids))return pt.matriz.ApiProblems.validation("responsibleUserIds", "Invalid responsible users.");
        List<Long> before=assignees(id);List<Long> sorted=new ArrayList<>(ids);Collections.sort(sorted);
        if(!before.equals(sorted)) {setAssignees(id,sorted);history(id,uid,"assignees",before.toString(),sorted.toString());}
        return ResponseEntity.ok(task(id));
    }
    @GetMapping("/tasks/{id}/comments")
    public ResponseEntity<?> comments(@PathVariable long id,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        if(taskAccess(id,uid)==null)return missing();
        var items=db.query("SELECT id,task_id,author_id,content,created_at FROM task_comments WHERE task_id=? ORDER BY created_at,id",
            (r,n)->new Comment(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getTimestamp(5).toInstant()),id);
        return ResponseEntity.ok(Map.of("items",items,"totalItems",items.size()));
    }
    @PostMapping("/tasks/{id}/comments") @Transactional
    public ResponseEntity<?> addComment(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        if(taskAccess(id,uid)==null)return missing();
        if(body==null||body.size()!=1||!body.containsKey("content"))return invalid();
        if(!(body.get("content") instanceof String s)||!text(s,1,2000))return pt.matriz.ApiProblems.validation("content", "Content must contain 1 to 2000 characters.");
        Long cid=db.queryForObject("INSERT INTO task_comments(task_id,author_id,content) VALUES (?,?,?) RETURNING id",Long.class,id,uid,s.strip());
        Comment comment=db.queryForObject("SELECT id,task_id,author_id,content,created_at FROM task_comments WHERE id=?",
            (r,n)->new Comment(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getTimestamp(5).toInstant()),cid);
        return ResponseEntity.created(URI.create("/api/v1/tasks/"+id+"/comments/"+cid)).body(comment);
    }
    private Object historyValue(String field, String value) {
        if (!"assignees".equals(field)) return value;
        // Stored assignment sets use List.toString(), including legacy history rows.
        String ids = value.substring(1, value.length() - 1).strip();
        return ids.isEmpty() ? List.of() : Arrays.stream(ids.split(","))
            .map(String::strip).map(Long::valueOf).toList();
    }
    @GetMapping("/tasks/{id}/history")
    public ResponseEntity<?> history(@PathVariable long id,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        if(taskAccess(id,uid)==null)return missing();
        var items=db.query("SELECT id,task_id,actor_id,field,previous_value,new_value,changed_at FROM task_history WHERE task_id=? ORDER BY changed_at,id",
            (r,n)->new History(r.getLong(1),r.getLong(2),r.getLong(3),
                "assignees".equals(r.getString(4)) ? "responsibleUserIds" : r.getString(4),
                historyValue(r.getString(4),r.getString(5)),historyValue(r.getString(4),r.getString(6)),
                r.getTimestamp(7).toInstant()),id);
        return ResponseEntity.ok(Map.of("items",items,"totalItems",items.size()));
    }
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestParam(required=false) String priority,Authentication auth) {
        Long uid=user(auth);if(uid==null)return pt.matriz.ApiProblems.response(401);
        if(priority!=null&&!priority(priority))return invalid();
        var projects=db.query("SELECT p.id,p.name,p.description,p.created_by_user_id FROM projects p JOIN project_memberships m ON m.project_id=p.id WHERE m.user_id=? ORDER BY p.id DESC",
            (r,n)->new Project(r.getLong(1),r.getString(2),r.getString(3),r.getObject(4,Long.class)),uid);
        List<Map<String,Object>> result=new ArrayList<>();long todo=0,progress=0,done=0;
        for(var p:projects){long pid=p.id();Counts c=counts(pid,priority);todo+=c.TODO();progress+=c.IN_PROGRESS();done+=c.DONE();
            Map<String,Object> entry=new LinkedHashMap<>();entry.put("project",p);entry.put("counts",c);result.add(entry);}
        return ResponseEntity.ok(Map.of("projects",result,"totalProjects",result.size(),"counts",new Counts(todo,progress,done,todo+progress+done)));
    }
}
