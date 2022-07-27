package site.lockeice.impl;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import site.lockeice.impl.service.*;

import java.util.List;

public class MyServiceFactory extends ServiceFactory {
    public MyServiceFactory() {
        super();
        registerService(CourseService.class, new MyCourseService());
        registerService(DepartmentService.class, new MyDepartmentService());
        registerService(InstructorService.class, new MyInstructorService());
        registerService(MajorService.class, new MyMajorService());
        registerService(SemesterService.class, new MySemesterService());
        registerService(StudentService.class, new MyStudentService());
        registerService(UserService.class, new MyUserService());
    }

    @Override
    public List<String> getUIDs() {
        return List.of("12110631");
    }
}
