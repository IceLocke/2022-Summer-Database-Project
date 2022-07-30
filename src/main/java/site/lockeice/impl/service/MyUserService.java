package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService {
    @Override
    public void removeUser(int userId) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            // remove week list
            String removeWeekList = """
                        delete from class_week_list
                        where class_timetable_id in (
                            select class_timetable_id
                            from class_timetable ctt
                            join class_teachers ct on ctt.class_timetable_id = ct.class_timetable_id
                            where teacher_id = ?
                        )
                    """;
            PreparedStatement s = conn.prepareStatement(removeWeekList);
            s.setInt(1, userId);
            s.execute();

            // remove class_timetable
            String removeClassTimetable = """
                        delete from class_timetable
                        where class_timetable_id in (
                            select class_timetable_id
                            from class_timetable ctt
                            join class_teachers ct on ctt.class_timetable_id = ct.class_timetable_id
                            where teacher_id = ?
                        )
                    """;
            s = conn.prepareStatement(removeClassTimetable);
            s.setInt(1, userId);
            s.execute();

            // remove user
            String removeUser = """
                        delete from teachers
                        where user_id = ?
                    """;
            s = conn.prepareStatement(removeUser);
            s.setInt(1, userId);
            s.execute();

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            ArrayList users = new ArrayList<User>();
            String sql = "select user_id, (first_name || ' ' || last_name) from teachers";
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(sql);
            while (res.next()) {
                User user = new Instructor();
                user.id = res.getInt(1);
                user.fullName = res.getString(2);
                users.add(user);
            }
            conn.close();
            return users;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
