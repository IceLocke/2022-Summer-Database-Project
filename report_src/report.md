# CS213 Project Report
by 12110631 ������

## Database Structure
### Task 5 Database
![](images/cs213project_database.png)
����Ϊ��Ĵ��¹��ܽ��ܡ�Task6��ɵĽӿ��в�δ���ǵ����е�����ֶΣ�����һ�������������ݣ��ɹ��Ժ���нӿ���չ��

| Table           | Description                                                               | Foreign Key                    |
|-----------------|---------------------------------------------------------------------------|--------------------------------|
| courses         | �洢ĳ���γ̣���Ӧ�ӿ�`Course`�����������                                                 | dept_id                        |
| classes         | �洢ĳ���γ̰༶����Ӧ�ӿ�`CourseSection`�����������                                        | course_id, semester_id         |
| class_timetable | �洢ĳ���γ̰༶�γ�ʱ�䣨��Ӧ�ӿ�`courseSectionClass`�����������                               | location_id, class_id          |
| class_teachers  | �洢ĳ���γ̰༶�γ�ʱ�����ʦ��������ݣ�����������`(class_timetable_id, teacher_id)`Լ��            | class_timetable_id, teacher_id |
| class_week_list | �洢ĳ���γ̰༶�γ�ʱ��Ķ�Ӧ��ѧ�ܵ��������                                                   | class_timetable_id             |
| students        | �洢ѧ�����������                                                                 | ad_class_id, major_id, user_id |
| course_select   | �洢ѧ��ѡ�ε��������                                                               | sid, class_id                  |
| teachers        | �洢��ʦ����Ӧ�ӿ�`Instructor`����ص����ݡ����ǽӿ���`Instructor` �̳��� `User��, ������¼Ϊ `user_id` | N/A                            |  
| ad_classes      | �洢ѧ���������Ӧ����Ϣ������ԭʼ����`student.csv`�������������Ӣ��������Ϊ�����ֶμ�¼��                      | N/A                            |
| departments     | �洢ѧУԺϵ���������                                                               | N/A                            |
| majors          | �洢ѧУרҵ���������                                                               | dept_id                        |
| course_type     | �洢ĳ��רҵ����ĳ���γ̵Ŀγ����͵��������                                                    | course_id, major_id            |
| semesters       | �洢ѧ�ڵ��������                                                                 | N/A                            |
| locations       | �洢�ص���������                                                                 | N/A                            |

���������е����⿼�ǽ���������

- `classes.left_capacity`: ���ʼ������У����ǵ�ʣ��γ���������ͨ���ۺϺ�����`course_select`�м���ó��������������ԣ���δ����ʵ�ʴ��ڡ����ڽ��нӿ�ʵ��ʱ������ʵ����`course_select`����������ǳ��޴󣬲���`addEnrolledCourseWithGrade`�ӿڵǼǵ�ѧ���γ̲�ռ�ÿγ�������ʹ�þۺϺ�������ͳ��ʱ����˺ܴ������ƿ������˿���ʹ�ÿռ任ȡʱ�䣬�ڽ��пγ̵Ǽǵ�ʱ����¸��ֶΡ�

### Task 1 Database
![](images/task1_database.png)
���Task5�е����ݿ⣬Task1�����ݿ�ȱ�������±�
- semesters: ѧ������
- majors: רҵ��Ϣ
- course_type: רҵ�Կγ̵ķ���

ĳЩ����ȱʡ�������ֶΣ�
- students: `major_id`, `first_name`, `last_name` (`name`���ϲ�Ϊ`student_name`), `enrolled_date`
- teachers: `first_name`, `last_name`(`name`���ϲ�Ϊ`teacher_name`)

ͬʱĳЩ������������ֶ�
- courses.course_id_suffix: �����Ͽƴ�γ�id����ĸǰ׺���п���Ժϵ��Ϣ���γ�id��ǰ׺������Ϊ�������ݣ���Task1������ݿ�ʱ�ҳ��Խ��γ�id���ΪԺϵid�Ϳγ�id��׺����`CS102A->{dept_id: CS, course_id_suffix: 102A}`����ͬʱʹ�����������������ƿγ�idΨһ���������Ľӿڿ����з��ֱ�֤�γ�id�������Ը������ڱ�д���룬����Task5�޸����ݿ�Ĺ����б�����������`course_id`��

���⣬`class_teachers`�е����Լ��Ҳ��ָ��`class_timetable_id`��Ϊ`class_id`����Ϊ`course_info.json`�в�û�жԿγ�ʱ�������ʦ�����֡�

## Data Importer Design