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
            statement.executeQuery();

            sql = "select max(major_id) from majors";
            Statement s = conn.createStatement();
            ResultSet res = s.executeQuery(sql);
            res.next();

            conn.commit();
            return res.getInt(1);
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = """
                        update courses
                        set course_type = 1
                        where course_id = ?
                    """;

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {

    }
}
