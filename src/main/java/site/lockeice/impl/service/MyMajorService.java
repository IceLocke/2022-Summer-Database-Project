package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;

public class MyMajorService implements MajorService {
    @Override
    @ParametersAreNonnullByDefault
    public int addMajor(String name, int departmentId) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = "insert into majors (major, dept_id) values(?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, name);
            statement.setInt(2, departmentId);
            statement.execute();

            sql = "select max(major_id) as max_major_id from majors";
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
    @ParametersAreNonnullByDefault
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        setCourseType(courseId, 1, majorId);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void addMajorElectiveCourse(int majorId, String courseId) {
        setCourseType(courseId, 2, majorId);
    }

    private void setCourseType(String courseId, int type, int majorID) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = """
                        insert into
                        course_type
                        (course_id, type, major_id)
                        values (?, ?, ?)
                    """;
            PreparedStatement s = conn.prepareStatement(sql);
            s.setString(1, courseId);
            s.setInt(2, type);
            s.setInt(3, majorID);
            s.execute();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
