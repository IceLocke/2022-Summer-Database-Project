package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyDepartmentService implements DepartmentService {

    @Override
    public int addDepartment(String name) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = "insert into departments (department) values ('%s')";
            Statement s = conn.createStatement();
            s.execute(sql.formatted(name));
            

            String queryCount = "select max(dept_id) from departments";
            s = conn.createStatement();
            ResultSet res = s.executeQuery(queryCount);
            res.next();

            int result = res.getInt(1);
            conn.close();
            return result;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void removeDepartment(int departmentId) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = "delete from majors where dept_id = %d";
            Statement s = conn.createStatement();
            s.execute(sql.formatted(departmentId));
            sql = "delete from departments where dept_id = %d";
            s = conn.createStatement();
            s.execute(sql.formatted(departmentId));
            conn.close();
            return;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            
            String sql = "select dept_id, department from departments";
            PreparedStatement s = conn.prepareStatement(sql);
            ResultSet res = s.executeQuery();
            ArrayList<Department> departments = new ArrayList<Department>();
            while (res.next()) {
                Department dept = new Department();
                dept.id = res.getInt(1);
                dept.name = res.getString(2);
                departments.add(dept);
            }
            conn.close();
            return departments;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
