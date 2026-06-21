package com.example.backend.config;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.Category;
import com.example.backend.entity.Role;
import com.example.backend.entity.Subject;
import com.example.backend.entity.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final CategoryRepository categoryRepository;

    public DatabaseInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository, SubjectRepository subjectRepository, CategoryRepository categoryRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.subjectRepository = subjectRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            Role admin = new Role(); admin.setRoleName(RoleType.ADMIN);
            Role user = new Role(); user.setRoleName(RoleType.STUDENT);
            Role teacher = new Role(); teacher.setRoleName(RoleType.TEACHER);
            roleRepository.saveAll(List.of(admin, user, teacher));
        }
        Role adminRole = roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Admin role not found"));
        Role teacherRole = roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.TEACHER)
                .orElseThrow(() -> new IllegalStateException("Teacher role not found"));
        Role studentRole = roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.STUDENT)
                .orElseThrow(() -> new IllegalStateException("Student role not found"));
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUserName("admin");
            admin.setFullName("ADMIN");
            admin.setGmail("admin@gmail.com");
            admin.setPhoneNumber("0123456789");
            admin.setStudentNumber("20052015");
            admin.setAddress("Hà Nội");
            admin.setPassword(new BCryptPasswordEncoder().encode("123"));
            admin.setRole(adminRole);
            admin.setVerified(true);
            User teacher1 = new User();
            teacher1.setUserName("teacher1");
            teacher1.setFullName("Phạm Kiên Định");
            teacher1.setGmail("teacher1@gmail.com");
            teacher1.setPhoneNumber("0310964076");
            teacher1.setStudentNumber("20050123");
            teacher1.setAddress("Hà Nội");
            teacher1.setPassword(new BCryptPasswordEncoder().encode("123"));
            teacher1.setRole(teacherRole);
            teacher1.setVerified(true);
            User teacher2 = new User();
            teacher2.setUserName("teacher2");
            teacher2.setFullName("Hoàng Hải Đăng");
            teacher2.setGmail("teacher2@gmail.com");
            teacher2.setPhoneNumber("0778410920");
            teacher2.setStudentNumber("20225174");
            teacher2.setAddress("Hà Nội");
            teacher2.setPassword(new BCryptPasswordEncoder().encode("123"));
            teacher2.setRole(teacherRole);
            teacher2.setVerified(true);
            User student0 = new User();
            student0.setUserName("student");
            student0.setFullName("STUDENT");
            student0.setGmail("student@gmail.com");
            student0.setPhoneNumber("0962644908");
            student0.setStudentNumber("20220123");
            student0.setAddress("Hà Nội");
            student0.setPassword(new BCryptPasswordEncoder().encode("123"));
            student0.setRole(studentRole);
            student0.setVerified(true);
            User student1 = new User();
            student1.setUserName("hayson");
            student1.setFullName("Hà Sơn");
            student1.setGmail("hason051004@gmail.com");
            student1.setPhoneNumber("0962644907");
            student1.setStudentNumber("20225388");
            student1.setAddress("Hà Nội");
            student1.setPassword(new BCryptPasswordEncoder().encode("123"));
            student1.setRole(studentRole);
            student1.setVerified(true);
            User student2 = new User();
            student2.setUserName("quando");
            student2.setFullName("Đỗ Thế Quân");
            student2.setGmail("quando.4002@gmail.com");
            student2.setPhoneNumber("0123456799");
            student2.setStudentNumber("20225384");
            student2.setAddress("Hà Nội");
            student2.setPassword(new BCryptPasswordEncoder().encode("123"));
            student2.setRole(studentRole);
            student2.setVerified(true);
            User student3 = new User();
            student3.setUserName("thuannguyen123");
            student3.setFullName("Nguyễn Ngọc Thuận");
            student3.setGmail("nguyenngocthuan940@gmail.com");
            student3.setPhoneNumber("0365373464");
            student3.setStudentNumber("20225413");
            student3.setAddress("Hà Nội");
            student3.setPassword(new BCryptPasswordEncoder().encode("123"));
            student3.setRole(studentRole);
            student3.setVerified(true);
            userRepository.saveAll(List.of(admin, teacher1, teacher2, student0, student1, student2, student3));
        }
        seedCategoriesAndSubjects();
    }

    private void seedCategoriesAndSubjects() {
        List<String> categoryTitles = List.of(
                "Khoa Giáo dục Quốc phòng và An ninh",
                "Khoa Giáo dục Thể chất",
                "Khoa Lý luận Chính trị",
                "Trường Cơ khí",
                "Trường Công nghệ Thông tin và Truyền thông",
                "Trường Điện - Điện tử",
                "Trường Hoá và Khoa học Sự sống",
                "Trường Kinh tế",
                "Trường Vật liệu",
                "Khoa Toán - Tin",
                "Khoa Vật lý Kỹ thuật",
                "Khoa Ngoại ngữ",
                "Khoa Khoa học và Công nghệ Giáo dục"
        );

        for (String title : categoryTitles) {
            categoryRepository.findByTitle(title).orElseGet(() -> {
                Category category = new Category();
                category.setTitle(title);
                return categoryRepository.save(category);
            });
        }

        Category ictCategory = categoryRepository.findByTitle("Trường Công nghệ Thông tin và Truyền thông")
                .orElseThrow(() -> new IllegalStateException("ICT category not found"));

        List<Subject> subjects = List.of(
                createSubject("IT3420", "Điện tử cho CNTT", ictCategory),
                createSubject("IT4785", "Phát triển ứng dụng cho thiết bị di động", ictCategory),
                createSubject("IT4735", "IoT và ứng dụng", ictCategory),
                createSubject("IT4681", "Truyền thông đa phương tiện", ictCategory),
                createSubject("IT4409", "Công nghệ Web và dịch vụ trực tuyến", ictCategory),
                createSubject("IT4263", "An ninh mạng", ictCategory),
                createSubject("IT3943", "Project III", ictCategory),
                createSubject("IT4991", "Thực tập kỹ thuật", ictCategory),
                createSubject("IT4651", "Thiết kế và triển khai mạng IP", ictCategory),
                createSubject("IT4611", "Các hệ thống phân tán và ứng dụng", ictCategory),
                createSubject("IT4060", "Lập trình mạng", ictCategory),
                createSubject("IT4015", "Nhập môn an toàn thông tin", ictCategory),
                createSubject("IT3931", "Project II", ictCategory),
                createSubject("IT3170", "Thuật toán ứng dụng", ictCategory),
                createSubject("IT3120", "Phân tích và thiết kế hệ thống", ictCategory),
                createSubject("IT3020", "Toán rời rạc", ictCategory),
                createSubject("IT4593", "Nhập môn kỹ thuật truyền thông", ictCategory),
                createSubject("IT4172", "Xử lý tín hiệu", ictCategory),
                createSubject("IT3180", "Nhập môn công nghệ phần mềm", ictCategory),
                createSubject("IT3150", "Project I", ictCategory),
                createSubject("IT3090", "Cơ sở dữ liệu", ictCategory),
                createSubject("IT3080", "Mạng máy tính", ictCategory),
                createSubject("IT3040", "Kỹ thuật lập trình", ictCategory)
        );

        for (Subject subject : subjects) {
            if (!subjectRepository.existsByCode(subject.getCode())) {
                subjectRepository.save(subject);
            }
        }
    }

    private Subject createSubject(String code, String title, Category category) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setTitle(title);
        subject.setCategory(category);
        return subject;
    }
}
