package api; 

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler; 
import com.sun.net.httpserver.HttpServer; 
import exception.AllocationException; 
import exception.ValidationException; 
import model.Allocation; 
import model.Exam; 
import model.Invigilator; 
import model.Room; 
import model.Student; 
import service.AllocationService; 
import service.ExamService; 
import service.InvigilatorService; 
import service.ReportService; 
import service.RoomService; 
import service.StudentService; 
import dao.AllocationDAO; 

import java.io.ByteArrayOutputStream; 
import java.io.IOException; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.net.InetSocketAddress; 
import java.net.URLDecoder; 
import java.nio.charset.StandardCharsets; 
import java.nio.file.Files; 
import java.nio.file.Path; 
import java.nio.file.Paths; 
import java.time.LocalDate; 
import java.util.ArrayList; 
import java.util.HashMap; 
import java.util.LinkedHashMap; 
import java.util.List; 
import java.util.Map; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 

public class ApiServer { 
    private final int port; 
    private HttpServer server; 
    private final StudentService studentService = new StudentService(); 
    private final RoomService roomService = new RoomService(); 
    private final ExamService examService = new ExamService(); 
    private final InvigilatorService invigilatorService = new InvigilatorService(); 
    private final AllocationService allocationService = new AllocationService(); 
    private final ReportService reportService = new ReportService(); 
    private final AllocationDAO allocationDAO = new AllocationDAO(); 

    public ApiServer(int port) { 
        this.port = port; 
    } 

    public void start() throws IOException { 
        server = HttpServer.create(new InetSocketAddress(port), 0); 
        
        // API Contexts
        HttpContext context = server.createContext("/api/health", exchange -> {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\",\"service\":\"Vyavastha API\"}");
        });

        server.createContext("/api/students", new StudentsHandler()); 
        server.createContext("/api/rooms", new RoomsHandler()); 
        server.createContext("/api/exams", new ExamsHandler()); 
        server.createContext("/api/invigilators", new InvigilatorsHandler()); 
        server.createContext("/api/allocate", new AllocateHandler()); 
        server.createContext("/api/allocations", new AllocationsHandler()); 
        server.createContext("/api/reports/chart", new ReportChartHandler()); 
        server.createContext("/api/reports/csv", new ReportCsvHandler()); 

        // Static React frontend context
        server.createContext("/", new StaticFileHandler()); 

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); 
        server.start(); 
        System.out.println("==================================================");
        System.out.println("  VYAVASTHA WEB APPLICATION RUNNING              ");
        System.out.println("  React Web UI : http://localhost:" + port);
        System.out.println("  REST API     : http://localhost:" + port + "/api/health");
        System.out.println("==================================================");
    } 

    public void stop() { 
        if (server != null) { 
            server.stop(0); 
        } 
    } 

    // --- Static File Handler (React UI) ---

    private class StaticFileHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            String path = exchange.getRequestURI().getPath(); 
            if (path.startsWith("/api/")) { 
                exchange.sendResponseHeaders(404, -1); 
                return; 
            } 

            if (path.equals("/") || path.isEmpty()) { 
                path = "/index.html"; 
            } 

            Path filePath = Paths.get("frontend/dist", path.replaceFirst("^/", "")); 
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) { 
                filePath = Paths.get("frontend/dist/index.html"); 
            } 

            if (!Files.exists(filePath)) { 
                String notFound = "Vyavastha: React build files not found in frontend/dist. Run 'npm run build' inside frontend/."; 
                byte[] b = notFound.getBytes(StandardCharsets.UTF_8); 
                exchange.sendResponseHeaders(404, b.length); 
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); } 
                return; 
            } 

            String contentType = "text/html; charset=UTF-8"; 
            String fn = filePath.getFileName().toString().toLowerCase(); 
            if (fn.endsWith(".js")) contentType = "application/javascript; charset=UTF-8"; 
            else if (fn.endsWith(".css")) contentType = "text/css; charset=UTF-8"; 
            else if (fn.endsWith(".json")) contentType = "application/json; charset=UTF-8"; 
            else if (fn.endsWith(".svg")) contentType = "image/svg+xml"; 
            else if (fn.endsWith(".png")) contentType = "image/png"; 

            byte[] bytes = Files.readAllBytes(filePath); 
            exchange.getResponseHeaders().set("Content-Type", contentType); 
            exchange.sendResponseHeaders(200, bytes.length); 
            try (OutputStream os = exchange.getResponseBody()) { 
                os.write(bytes); 
            } 
        } 
    } 

    // --- Handlers --- 

    private class StudentsHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            String method = exchange.getRequestMethod(); 
            if ("OPTIONS".equalsIgnoreCase(method)) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                if ("GET".equalsIgnoreCase(method)) { 
                    List<Student> list = studentService.list(); 
                    StringBuilder sb = new StringBuilder("["); 
                    for (int i = 0; i < list.size(); i++) { 
                        Student s = list.get(i); 
                        sb.append(String.format("{\"rollNo\":%d,\"name\":\"%s\",\"subjectCode\":\"%s\"}", 
                            s.getRollNo(), escape(s.getName()), escape(s.getSubjectCode()))); 
                        if (i < list.size() - 1) sb.append(","); 
                    } 
                    sb.append("]"); 
                    sendJson(exchange, 200, sb.toString()); 
                } else if ("POST".equalsIgnoreCase(method)) { 
                    String body = readBody(exchange); 
                    int roll = getIntField(body, "rollNo"); 
                    String name = getStringField(body, "name"); 
                    String subject = getStringField(body, "subjectCode"); 
                    studentService.add(roll, name, subject); 
                    sendJson(exchange, 201, "{\"message\":\"Student added successfully\"}"); 
                } else if ("DELETE".equalsIgnoreCase(method)) { 
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                    int roll = Integer.parseInt(q.get("rollNo")); 
                    studentService.delete(roll); 
                    sendJson(exchange, 200, "{\"message\":\"Student deleted successfully\"}"); 
                } else { 
                    exchange.sendResponseHeaders(405, -1); 
                } 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class RoomsHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            String method = exchange.getRequestMethod(); 
            if ("OPTIONS".equalsIgnoreCase(method)) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                if ("GET".equalsIgnoreCase(method)) { 
                    List<Room> list = roomService.list(); 
                    StringBuilder sb = new StringBuilder("["); 
                    for (int i = 0; i < list.size(); i++) { 
                        Room r = list.get(i); 
                        sb.append(String.format("{\"roomId\":%d,\"roomName\":\"%s\",\"rows\":%d,\"cols\":%d,\"capacity\":%d}", 
                            r.getRoomId(), escape(r.getRoomName()), r.getRows(), r.getCols(), r.capacity())); 
                        if (i < list.size() - 1) sb.append(","); 
                    } 
                    sb.append("]"); 
                    sendJson(exchange, 200, sb.toString()); 
                } else if ("POST".equalsIgnoreCase(method)) { 
                    String body = readBody(exchange); 
                    String name = getStringField(body, "roomName"); 
                    int rows = getIntField(body, "rows"); 
                    int cols = getIntField(body, "cols"); 
                    roomService.add(name, rows, cols); 
                    sendJson(exchange, 201, "{\"message\":\"Room added successfully\"}"); 
                } else if ("DELETE".equalsIgnoreCase(method)) { 
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                    int id = Integer.parseInt(q.get("roomId")); 
                    roomService.delete(id); 
                    sendJson(exchange, 200, "{\"message\":\"Room deleted successfully\"}"); 
                } else { 
                    exchange.sendResponseHeaders(405, -1); 
                } 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class ExamsHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            String method = exchange.getRequestMethod(); 
            if ("OPTIONS".equalsIgnoreCase(method)) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                if ("GET".equalsIgnoreCase(method)) { 
                    List<Exam> list = examService.list(); 
                    StringBuilder sb = new StringBuilder("["); 
                    for (int i = 0; i < list.size(); i++) { 
                        Exam e = list.get(i); 
                        sb.append(String.format("{\"examId\":%d,\"subjectCode\":\"%s\",\"examDate\":\"%s\",\"slot\":\"%s\"}", 
                            e.getExamId(), escape(e.getSubjectCode()), e.getExamDate(), escape(e.getSlot()))); 
                        if (i < list.size() - 1) sb.append(","); 
                    } 
                    sb.append("]"); 
                    sendJson(exchange, 200, sb.toString()); 
                } else if ("POST".equalsIgnoreCase(method)) { 
                    String body = readBody(exchange); 
                    String subject = getStringField(body, "subjectCode"); 
                    String date = getStringField(body, "examDate"); 
                    String slot = getStringField(body, "slot"); 
                    examService.add(subject, date, slot); 
                    sendJson(exchange, 201, "{\"message\":\"Exam added successfully\"}"); 
                } else if ("DELETE".equalsIgnoreCase(method)) { 
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                    int id = Integer.parseInt(q.get("examId")); 
                    examService.delete(id); 
                    sendJson(exchange, 200, "{\"message\":\"Exam deleted successfully\"}"); 
                } else { 
                    exchange.sendResponseHeaders(405, -1); 
                } 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class InvigilatorsHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            String method = exchange.getRequestMethod(); 
            if ("OPTIONS".equalsIgnoreCase(method)) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                if ("GET".equalsIgnoreCase(method)) { 
                    List<Invigilator> list = invigilatorService.list(); 
                    StringBuilder sb = new StringBuilder("["); 
                    for (int i = 0; i < list.size(); i++) { 
                        Invigilator inv = list.get(i); 
                        sb.append(String.format("{\"invigilatorId\":%d,\"name\":\"%s\"}", 
                            inv.getInvigilatorId(), escape(inv.getName()))); 
                        if (i < list.size() - 1) sb.append(","); 
                    } 
                    sb.append("]"); 
                    sendJson(exchange, 200, sb.toString()); 
                } else if ("POST".equalsIgnoreCase(method)) { 
                    String body = readBody(exchange); 
                    String name = getStringField(body, "name"); 
                    invigilatorService.add(name); 
                    sendJson(exchange, 201, "{\"message\":\"Invigilator added successfully\"}"); 
                } else if ("DELETE".equalsIgnoreCase(method)) { 
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                    int id = Integer.parseInt(q.get("invigilatorId")); 
                    invigilatorService.delete(id); 
                    sendJson(exchange, 200, "{\"message\":\"Invigilator deleted successfully\"}"); 
                } else { 
                    exchange.sendResponseHeaders(405, -1); 
                } 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class AllocateHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            String method = exchange.getRequestMethod(); 
            if ("OPTIONS".equalsIgnoreCase(method)) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                if ("POST".equalsIgnoreCase(method)) { 
                    String body = readBody(exchange); 
                    String dateStr = getStringField(body, "date"); 
                    String slot = getStringField(body, "slot"); 
                    LocalDate date = ExamService.parseDate(dateStr); 

                    AllocationService.Result res = allocationService.allocateSession(date, slot); 
                    StringBuilder sb = new StringBuilder(); 
                    sb.append("{"); 
                    sb.append("\"date\":\"").append(date).append("\","); 
                    sb.append("\"slot\":\"").append(escape(slot)).append("\","); 
                    sb.append("\"seated\":").append(res.allocations.size()).append(","); 
                    sb.append("\"roomsUsed\":").append(res.invigilators.size()).append(","); 

                    // Warnings 
                    sb.append("\"warnings\":["); 
                    for (int i = 0; i < res.warnings.size(); i++) { 
                        sb.append("\"").append(escape(res.warnings.get(i))).append("\""); 
                        if (i < res.warnings.size() - 1) sb.append(","); 
                    } 
                    sb.append("],"); 

                    // Invigilators 
                    sb.append("\"invigilators\":["); 
                    int idx = 0; 
                    for (Map.Entry<Room, Invigilator> e : res.invigilators.entrySet()) { 
                        sb.append(String.format("{\"room\":\"%s\",\"invigilator\":\"%s\"}", 
                            escape(e.getKey().getRoomName()), escape(e.getValue().getName()))); 
                        if (++idx < res.invigilators.size()) sb.append(","); 
                    } 
                    sb.append("]"); 
                    sb.append("}"); 
                    sendJson(exchange, 200, sb.toString()); 
                } else if ("DELETE".equalsIgnoreCase(method)) { 
                    Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                    LocalDate date = ExamService.parseDate(q.get("date")); 
                    String slot = q.get("slot"); 
                    allocationService.clearSession(date, slot); 
                    sendJson(exchange, 200, "{\"message\":\"Session cleared successfully\"}"); 
                } else { 
                    exchange.sendResponseHeaders(405, -1); 
                } 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class AllocationsHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            String method = exchange.getRequestMethod(); 
            if ("OPTIONS".equalsIgnoreCase(method)) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                LocalDate date = ExamService.parseDate(q.get("date")); 
                String slot = q.get("slot"); 

                List<Allocation> list = allocationDAO.findBySession(date, slot); 
                Map<Integer, String> invigilators = allocationDAO.findInvigilators(date, slot); 

                Map<Room, List<Allocation>> byRoom = new LinkedHashMap<>(); 
                for (Allocation a : list) { 
                    byRoom.computeIfAbsent(a.getSeat().getRoom(), k -> new ArrayList<>()).add(a); 
                } 

                StringBuilder sb = new StringBuilder(); 
                sb.append("{\"date\":\"").append(date).append("\",\"slot\":\"").append(escape(slot)).append("\","); 
                sb.append("\"totalSeated\":").append(list.size()).append(","); 
                sb.append("\"rooms\":["); 

                int rIdx = 0; 
                for (Map.Entry<Room, List<Allocation>> entry : byRoom.entrySet()) { 
                    Room room = entry.getKey(); 
                    List<Allocation> roomAlloc = entry.getValue(); 
                    String invName = invigilators.getOrDefault(room.getRoomId(), "(Unassigned)"); 

                    sb.append("{"); 
                    sb.append(String.format("\"roomId\":%d,\"roomName\":\"%s\",\"rows\":%d,\"cols\":%d,\"invigilator\":\"%s\",", 
                        room.getRoomId(), escape(room.getRoomName()), room.getRows(), room.getCols(), escape(invName))); 
                    sb.append("\"seats\":["); 

                    for (int sIdx = 0; sIdx < roomAlloc.size(); sIdx++) { 
                        Allocation a = roomAlloc.get(sIdx); 
                        sb.append(String.format("{\"row\":%d,\"col\":%d,\"rollNo\":%d,\"name\":\"%s\",\"subjectCode\":\"%s\"}", 
                            a.getSeat().getRow(), a.getSeat().getCol(), 
                            a.getStudent().getRollNo(), escape(a.getStudent().getName()), escape(a.getStudent().getSubjectCode()))); 
                        if (sIdx < roomAlloc.size() - 1) sb.append(","); 
                    } 
                    sb.append("]}"); 
                    if (++rIdx < byRoom.size()) sb.append(","); 
                } 
                sb.append("]}"); 
                sendJson(exchange, 200, sb.toString()); 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class ReportChartHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                LocalDate date = ExamService.parseDate(q.get("date")); 
                String slot = q.get("slot"); 
                String chart = reportService.seatingChart(date, slot); 
                sendJson(exchange, 200, "{\"chart\":\"" + escape(chart).replace("\n", "\\n") + "\"}"); 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    private class ReportCsvHandler implements HttpHandler { 
        @Override 
        public void handle(HttpExchange exchange) throws IOException { 
            addCorsHeaders(exchange); 
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; } 

            try { 
                Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery()); 
                LocalDate date = ExamService.parseDate(q.get("date")); 
                String slot = q.get("slot"); 
                Path file = reportService.exportCsv(date, slot); 
                byte[] bytes = Files.readAllBytes(file); 
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8"); 
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.getFileName().toString() + "\""); 
                exchange.sendResponseHeaders(200, bytes.length); 
                try (OutputStream os = exchange.getResponseBody()) { 
                    os.write(bytes); 
                } 
            } catch (RuntimeException e) { 
                handleException(exchange, e); 
            } 
        } 
    } 

    // --- Helpers --- 

    private void addCorsHeaders(HttpExchange exchange) { 
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); 
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS"); 
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization"); 
    } 

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException { 
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8); 
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8"); 
        exchange.sendResponseHeaders(status, bytes.length); 
        try (OutputStream os = exchange.getResponseBody()) { 
            os.write(bytes); 
        } 
    } 

    private void handleException(HttpExchange exchange, RuntimeException ex) throws IOException { 
        int status = (ex instanceof ValidationException || ex instanceof AllocationException) ? 400 : 500; 
        String msg = escape(ex.getMessage() == null ? "Unexpected error" : ex.getMessage()); 
        sendJson(exchange, status, "{\"error\":\"" + msg + "\"}"); 
    } 

    private String readBody(HttpExchange exchange) throws IOException { 
        try (InputStream is = exchange.getRequestBody(); 
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) { 
            byte[] buf = new byte[1024]; 
            int n; 
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n); 
            return baos.toString(StandardCharsets.UTF_8); 
        } 
    } 

    private Map<String, String> parseQuery(String query) { 
        Map<String, String> map = new HashMap<>(); 
        if (query == null || query.isBlank()) return map; 
        for (String param : query.split("&")) { 
            String[] pair = param.split("="); 
            if (pair.length > 1) { 
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), 
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8)); 
            } 
        } 
        return map; 
    } 

    private String getStringField(String json, String key) { 
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\""); 
        Matcher m = p.matcher(json); 
        if (m.find()) return m.group(1); 
        return ""; 
    } 

    private int getIntField(String json, String key) { 
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)"); 
        Matcher m = p.matcher(json); 
        if (m.find()) return Integer.parseInt(m.group(1)); 
        return 0; 
    } 

    private String escape(String s) { 
        if (s == null) return ""; 
        return s.replace("\\", "\\\\") 
                .replace("\"", "\\\"") 
                .replace("\r", ""); 
    } 

    public static void main(String[] args) throws Exception { 
        int port = 8080; 
        if (args.length > 0) port = Integer.parseInt(args[0]); 
        ApiServer server = new ApiServer(port); 
        server.start(); 
    } 
}
