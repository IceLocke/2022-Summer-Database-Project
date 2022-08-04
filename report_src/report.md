# CS213 Project Report
by 12110631 龚凌琥

## Database Structure
### Task 5 Database
![](images/cs213project_database.png)
以下为表的大致功能介绍。Task6完成的接口中并未覆盖到所有的相关字段，存在一定量的冗余数据，可供以后进行接口拓展。

| Table           | Description                                                               | Foreign Key                    |
|-----------------|---------------------------------------------------------------------------|--------------------------------|
| courses         | 存储某个课程（对应接口`Course`）的相关数据                                                 | dept_id                        |
| classes         | 存储某个课程班级（对应接口`CourseSection`）的相关数据                                        | course_id, semester_id         |
| class_timetable | 存储某个课程班级课程时间（对应接口`courseSectionClass`）的相关数据                               | location_id, class_id          |
| class_teachers  | 存储某个课程班级课程时间的老师的相关数据，以联合主键`(class_timetable_id, teacher_id)`约束            | class_timetable_id, teacher_id |
| class_week_list | 存储某个课程班级课程时间的对应教学周的相关数据                                                   | class_timetable_id             |
| students        | 存储学生的相关数据                                                                 | ad_class_id, major_id, user_id |
| course_select   | 存储学生选课的相关数据                                                               | sid, class_id                  |
| teachers        | 存储老师（对应接口`Instructor`）相关的数据。考虑接口中`Instructor` 继承自 `User·, 主键记录为 `user_id` | N/A                            |  
| ad_classes      | 存储学生行政班对应的信息。由于原始数据`student.csv`中行政班具有中英文名，分为两个字段记录。                      | N/A                            |
| departments     | 存储学校院系的相关数据                                                               | N/A                            |
| majors          | 存储学校专业的相关数据                                                               | dept_id                        |
| course_type     | 存储某个专业划分某个课程的课程类型的相关数据                                                    | course_id, major_id            |
| semesters       | 存储学期的相关数据                                                                 | N/A                            |
| locations       | 存储地点的相关数据                                                                 | N/A                            |

接下来对列的特殊考虑进行描述。

- `classes.left_capacity`: 在最开始的设计中，考虑到剩余课程容量可以通过聚合函数从`course_select`中计算得出，属于派生属性，并未考虑实际存在。但在进行接口实现时，发现实际上`course_select`表的数据量非常巨大，并且`addEnrolledCourseWithGrade`接口登记的学生课程不占用课程容量，使用聚合函数进行统计时造成了很大的性能瓶颈，因此考虑使用空间换取时间，在进行课程登记的时候更新该字段。

### Task 1 Database
![](images/task1_database.png)
相比Task5中的数据库，Task1的数据库缺少了以下表：
- semesters: 学期数据
- majors: 专业信息
- course_type: 专业对课程的分类

某些表中缺省了以下字段：
- students: `major_id`, `first_name`, `last_name` (`name`被合并为`student_name`), `enrolled_date`
- teachers: `first_name`, `last_name`(`name`被合并为`teacher_name`)

同时某些表添加了以下字段
- courses.course_id_suffix: 由于南科大课程id的字母前缀带有开课院系信息，课程id的前缀或许会成为冗余数据，在Task1设计数据库时我尝试将课程id拆分为院系id和课程id后缀（如`CS102A->{dept_id: CS, course_id_suffix: 102A}`），同时使用联合联合主键限制课程id唯一。但后续的接口开发中发现保证课程id的完整性更有助于编写代码，遂在Task5修改数据库的过程中保留了完整的`course_id`。

此外，`class_teachers`中的外键约束也从指向`class_timetable_id`改为`class_id`，因为`course_info.json`中并没有对课程时间进行老师的区分。

## Data Importer Design
我使用了Python开发导入数据的脚本，主要使用了`psycopg`模块连接数据库，`json`模块对`course_info.info`进行解析，`select_course.csv`则直接使用文件IO进行字符串处理。

脚本分为两个模块，`course_import.py`和`student_import.py`，以下为部分核心代码

```python
# course_import.py
import psycopg

departments = []
teachers = ['null']
locations = []
course_ids = []
courses = []

# ...
# 对json进行数据预处理

with psycopg.connect('host=localhost port=5432 dbname=cs213project '
                     'user=postgres password=0906KOORI') as conn:
    with conn.cursor() as cur:
        for i, department in enumerate(departments):
            cur.execute("insert into departments (dept_id, department) values(%d, '%s')" %
                        (i, department))

        cur.execute("insert into teachers (user_id, first_name, last_name) values(0, null, null)")
        for i, teacher in enumerate(teachers):
            split_name = teacher.split(' ')
            if len(split_name) > 1:
                first_name = split_name[0]
                last_name = split_name[-1]
            else:
                first_name = teachers[i][1:]
                last_name = teachers[i][0]
            cur.execute("insert into teachers "
                        "(user_id, first_name, last_name) "
                        "values(%d, '%s', '%s')" %
                        (i + 1, first_name, last_name))

        for i, location in enumerate(locations):
            cur.execute("insert into locations (location_id, location) values(%d, '%s')" % (i, location))

        for i, course in enumerate(courses):
            cur.execute("insert into courses (course_id,  dept_id, "
                        "course_name, credit, hour, prerequisite)"
                        "values ('%s', %d, '%s', %d, %d, %s)" %
                        (
                            course['course_id'],
                            course['course_dept_id'],
                            course['course_name'],
                            course['credit'],
                            course['hour'],
                            'null' if course['prerequisite'] is None else ("'%s'" % course['prerequisite']))
                        )

        tt_id = 0
        for i in range(len(course_info)):
            cur.execute("""
                        insert into classes (class_id, course_id, class_name, capacity, semester_id)
                                    values (%d, '%s', '%s', %d, %d)
                        """ %
                        (
                            i,
                            course_info[i]['courseId'],
                            course_info[i]['className'].strip(),
                            course_info[i]['totalCapacity'],
                            0
                        ))

            for cl in course_info[i]['classList']:
                cur.execute('insert into class_timetable '
                            '(class_timetable_id, class_id, location_id, time_begin, time_end, weekday) '
                            'values(%d, %d, %d, %d, %d, %d)' %
                            (
                                tt_id,
                                i,
                                locations.index(cl['location']),
                                int(cl['classTime'].split('-')[0]),
                                int(cl['classTime'].split('-')[1]),
                                cl['weekday']
                            ))
                for w in cl['weekList']:
                    cur.execute('insert into class_week_list (class_timetable_id, week) '
                                'values(%d, %d)' %
                                (tt_id, int(w)))
                if course_info[i]['teacher'] is not None:
                    t_list = course_info[i]['teacher'].strip().split(',')
                    for t in t_list:
                        cur.execute("insert into class_teachers "
                                    "(class_timetable_id, teacher_id) values(%d, %d)" %
                                    (tt_id, teachers.index(t)))
                else:
                    cur.execute("insert into class_teachers "
                                "(class_timetable_id, teacher_id) values(%d, null)" % tt_id)
                tt_id = tt_id + 1

    conn.commit()
```

```python
with open('data/select_course.csv', 'r', encoding='utf-8') as f:
    with psycopg.connect('host=localhost port=5432 dbname=cs213project '
                         'user=postgres password=0906KOORI') as conn:
        lines = f.readlines(10000)
        cur = conn.cursor()

        cur.execute("select course_id as full_course_id from courses")
        for row in cur.fetchall():
            courses.append(row[0])

        for course in courses:
            cur.execute("select class_id from classes where course_id = '%s' limit 1" % course)
            class_map[course] = cur.fetchone()[0]

        for i, line in enumerate(lines):
            print(i)
            data = line.split(',')
            cnn = data[2].split('(')[0]
            enn = re.findall(r'\((.*?)\)', data[2])[0]
            if cnn not in cn_name:
                cn_name.append(cnn)
                eng_name.append(enn)
                try:
                    cur.execute("insert into ad_classes (ad_class_id, ad_class_chinese_name, ad_class_english_name)"
                                "values (%d, '%s', '%s')" %
                                (
                                    len(cn_name) - 1,
                                    cnn,
                                    enn
                                ))
                except psycopg.Error as e:
                    print(e)
            try:
                cur.execute(("""
                            insert into students 
                            (first_name, last_name, gender, ad_class_id, sid, major_id, user_id)
                            values ('%s', '%s', '%s', %d, %d, %d, %d)
                        """ %
                          (
                              str(data[0][1:]),
                              data[0][0],
                              data[1],
                              cn_name.index(data[2].split('(')[0]),
                              int(data[3]),
                              random.randint(1, 34),
                              random.randint(1, 100)
                          )))

            except psycopg.Error as e:
                print(e)
            for course in data[4:]:
                if course.strip() in courses:
                    try:
                        # 添加单条选课信息
                        cur.execute("""
                            insert into course_select (sid, class_id, grade)
                            values(%d, '%s', %d)
                        """ % (
                            int(data[3]),
                            class_map[course.strip()],
                            random.randint(59, 100)
                        ))
                    except psycopg.Error as e:
                        print(e)
        conn.commit()
```
由于`course_select.csv`数据量较大，单线程导入需要较长的时间，接近80分钟才能导入完480w条数据（Desktop PC，CPU: R7 3700X @3.6Ghz，8C16T，RAM: 8G * 2 @4000Mhz），考虑到对CPU核心的充分利用，我使用`thread`模块对于学生添加单条选课信息的部分进行了多线程的优化，将80分钟缩短至9分钟。

以下为部分修改代码：
```python
try:
    _thread.start_new_thread(cur.execute,
                             ("""
                                insert into students 
                                (first_name, last_name, gender, ad_class_id, sid, major_id, user_id)
                                values ('%s', '%s', '%s', %d, %d, %d, %d)
                            """ %
                              (
                                  str(data[0][1:]),
                                  data[0][0],
                                  data[1],
                                  cn_name.index(data[2].split('(')[0]),
                                  int(data[3]),
                                  random.randint(1, 34),
                                  random.randint(1, 100)
                              ),))
except psycopg.Error as e:
    print(e)
for course in data[4:]:
    if course.strip() in courses:
        try:
            _thread.start_new_thread(
                cur.execute, (f"""
                            insert into course_select (sid, class_id, grade)
                            values({int(data[3]):d}, '{class_map[course.strip()]}', {random.randint(59, 100):d})
                        """,)
            )
        except psycopg.Error as e:
            print(e)
```
