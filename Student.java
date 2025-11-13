import java.io.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class StudentServlet extends HttpServlet {

    private List<Student> students = new ArrayList<>();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html><head><title>Student Management</title></head><body style='font-family:sans-serif;'>");
        out.println("<h1 style='color:purple;text-align:center;'>Student Management Web App 🌟</h1>");

        out.println("<h2>Add Student</h2>");
        out.println("<form method='post' action='students'>");
        out.println("ID: <input type='text' name='id'><br><br>");
        out.println("Name: <input type='text' name='name'><br><br>");
        out.println("Address: <input type='text' name='address'><br><br>");
        out.println("<input type='submit' value='Add Student'>");
        out.println("</form>");

        out.println("<h2>Student List</h2>");
        if (students.isEmpty()) {
            out.println("<p>No students added yet.</p>");
        } else {
            out.println("<table border='1' cellpadding='8' style='border-collapse:collapse;'>");
            out.println("<tr><th>ID</th><th>Name</th><th>Address</th></tr>");
            for (Student s : students) {
                out.println("<tr><td>" + s.getId() + "</td><td>" + s.getName() + "</td><td>" + s.getAddress() + "</td></tr>");
            }
            out.println("</table>");
        }

        out.println("</body></html>");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        String name = request.getParameter("name");
        String address = request.getParameter("address");

        if (id != null && name != null && address != null && !id.isEmpty() && !name.isEmpty() && !address.isEmpty()) {
            students.add(new Student(id, name, address));
        }

        response.sendRedirect("students");
    }

    static class Student {
        private String id;
        private String name;
        private String address;

        public Student(String id, String name, String address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
    }
}
