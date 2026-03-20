package com.example.backend.config;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public DatabaseInitializer(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            Role admin = new Role(); admin.setRoleName(RoleType.ADMIN);
            Role user = new Role(); user.setRoleName(RoleType.STUDENT);
            Role teacher = new Role(); teacher.setRoleName(RoleType.TEACHER);
            roleRepository.saveAll(List.of(admin, user, teacher));
        }
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUserName("admin");
            admin.setFullName("ADMIN");
            admin.setGmail("admin@gmail.com");
            admin.setPhoneNumber("0123456789");
            admin.setStudentNumber("20052015");
            admin.setAddress("Hà Nội");
            admin.setPassword(new BCryptPasswordEncoder().encode("123456"));
            admin.setRole(roleRepository.findByRoleName(RoleType.ADMIN));
            admin.setVerified(true);
            User teacher1 = new User();
            teacher1.setUserName("teacher1");
            teacher1.setFullName("Phạm Kiên Định");
            teacher1.setGmail("teacher1@gmail.com");
            teacher1.setPhoneNumber("0310964076");
            teacher1.setStudentNumber("20050123");
            teacher1.setAddress("Hà Nội");
            teacher1.setPassword(new BCryptPasswordEncoder().encode("123456"));
            teacher1.setRole(roleRepository.findByRoleName(RoleType.TEACHER));
            teacher1.setVerified(true);
            User teacher2 = new User();
            teacher2.setUserName("teacher2");
            teacher2.setFullName("Hoàng Hải Đăng");
            teacher2.setGmail("teacher2@gmail.com");
            teacher2.setPhoneNumber("0778410920");
            teacher2.setStudentNumber("20225174");
            teacher2.setAddress("Hà Nội");
            teacher2.setPassword(new BCryptPasswordEncoder().encode("123456"));
            teacher2.setRole(roleRepository.findByRoleName(RoleType.TEACHER));
            teacher2.setVerified(true);
            User student0 = new User();
            student0.setUserName("student");
            student0.setFullName("STUDENT");
            student0.setGmail("student@gmail.com");
            student0.setPhoneNumber("0962644908");
            student0.setStudentNumber("20220123");
            student0.setAddress("Hà Nội");
            student0.setPassword(new BCryptPasswordEncoder().encode("123456"));
            student0.setRole(roleRepository.findByRoleName(RoleType.STUDENT));
            student0.setVerified(true);
            User student1 = new User();
            student1.setUserName("hayson");
            student1.setFullName("Hà Sơn");
            student1.setGmail("hason051004@gmail.com");
            student1.setPhoneNumber("0962644907");
            student1.setStudentNumber("20225388");
            student1.setAddress("Hà Nội");
            student1.setPassword(new BCryptPasswordEncoder().encode("123456"));
            student1.setRole(roleRepository.findByRoleName(RoleType.STUDENT));
            student1.setVerified(true);
            User student2 = new User();
            student2.setUserName("quando");
            student2.setFullName("Đỗ Thế Quân");
            student2.setGmail("quando.4002@gmail.com");
            student2.setPhoneNumber("0123456799");
            student2.setStudentNumber("20225384");
            student2.setAddress("Hà Nội");
            student2.setPassword(new BCryptPasswordEncoder().encode("123456"));
            student2.setRole(roleRepository.findByRoleName(RoleType.STUDENT));
            student2.setVerified(true);
            User student3 = new User();
            student3.setUserName("thuannguyen123");
            student3.setFullName("Nguyễn Ngọc Thuận");
            student3.setGmail("nguyenngocthuan940@gmail.com");
            student3.setPhoneNumber("0365373464");
            student3.setStudentNumber("20225413");
            student3.setAddress("Hà Nội");
            student3.setPassword(new BCryptPasswordEncoder().encode("123456"));
            student3.setRole(roleRepository.findByRoleName(RoleType.STUDENT));
            student3.setVerified(true);
            userRepository.saveAll(List.of(admin, teacher1, teacher2, student0, student1, student2, student3));
        }
    }
}
