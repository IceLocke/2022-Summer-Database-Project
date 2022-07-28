package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import java.sql.*;

public class MyMajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "insert into majors (major, dept_id) values(?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, name);
            statement.setInt(2, departmentId);
            statement.execute();

            sql = "select max(major_id) from majors";
            Statement s = conn.createStatement();
            ResultSet res = s.executeQuery(sql);
            res.next();

            int result = res.getInt(1);
            conn.close();
            return result;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        setCourseType(courseId, 1);
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        setCourseType(courseId, 2);
    }

    private void setCourseType(String courseId, int type) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = """
                        update courses
                        set course_type = ?
                        where course_id = ?
                    """;
            PreparedStatement s = conn.prepareStatement(sql);
            s.setInt(1, type);
            s.setString(2, courseId);
            s.execute();
            conn.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
