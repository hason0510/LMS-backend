package com.example.backend.cache;

import java.util.List;

public final class CacheNames {
    public static final String USER = "user";
    public static final String USER_PAGE = "user_page";

    public static final String TEACHING_CONTEXT = "teaching_context";
    public static final String TEACHING_CLASSES = "teaching_classes";
    public static final String TEACHING_WORKBENCH_SUMMARY = "teaching_workbench_summary";
    public static final String TEACHING_REVIEW_QUEUE = "teaching_review_queue";
    public static final String TEACHING_CLASS_PEOPLE = "teaching_class_people";

    public static final String CLASS_SECTION_DETAIL = "class_section_detail";
    public static final String CLASS_SECTION_LIST = "class_section_list";
    public static final String CLASS_SECTION_SEARCH = "class_section_search";
    public static final String STUDENT_CLASS_SECTION_LIST = "student_class_section_list";

    public static final String ENROLLMENT_TEACHER = "enrollment_teacher";
    public static final String ENROLLMENT_APPROVED_CLASS_SECTION = "enrollment_approved_class_section";
    public static final String ENROLLMENT_PENDING_CLASS_SECTION = "enrollment_pending_class_section";

    public static final String QUIZ_GRADEBOOK_CLASS_SECTION = "quiz_gradebook_class_section";
    public static final String QUIZ_GRADEBOOK_COURSE = "quiz_gradebook_course";
    public static final String ASSIGNMENT_TEACHING_OVERVIEW = "assignment_teaching_overview";

    public static final String ADMIN_REPORT_SUMMARY = "admin_report_summary";
    public static final String CLASS_REPORT_OVERVIEW = "class_report_overview";
    public static final String CLASS_ASSIGNMENT_REPORT = "class_assignment_report";

    public static final List<String> USER_CACHES = List.of(
            USER,
            USER_PAGE
    );

    public static final List<String> TEACHING_CACHES = List.of(
            TEACHING_CONTEXT,
            TEACHING_CLASSES,
            TEACHING_WORKBENCH_SUMMARY,
            TEACHING_REVIEW_QUEUE,
            TEACHING_CLASS_PEOPLE
    );

    public static final List<String> CLASS_SECTION_CACHES = List.of(
            CLASS_SECTION_DETAIL,
            CLASS_SECTION_LIST,
            CLASS_SECTION_SEARCH,
            STUDENT_CLASS_SECTION_LIST
    );

    public static final List<String> REPORT_CACHES = List.of(
            ENROLLMENT_TEACHER,
            ENROLLMENT_APPROVED_CLASS_SECTION,
            ENROLLMENT_PENDING_CLASS_SECTION,
            QUIZ_GRADEBOOK_CLASS_SECTION,
            QUIZ_GRADEBOOK_COURSE,
            ASSIGNMENT_TEACHING_OVERVIEW,
            ADMIN_REPORT_SUMMARY,
            CLASS_REPORT_OVERVIEW,
            CLASS_ASSIGNMENT_REPORT
    );

    public static final List<String> ALL_REDIS_READ_CACHES = List.of(
            USER,
            USER_PAGE,
            TEACHING_CONTEXT,
            TEACHING_CLASSES,
            TEACHING_WORKBENCH_SUMMARY,
            TEACHING_REVIEW_QUEUE,
            TEACHING_CLASS_PEOPLE,
            CLASS_SECTION_DETAIL,
            CLASS_SECTION_LIST,
            CLASS_SECTION_SEARCH,
            STUDENT_CLASS_SECTION_LIST,
            ENROLLMENT_TEACHER,
            ENROLLMENT_APPROVED_CLASS_SECTION,
            ENROLLMENT_PENDING_CLASS_SECTION,
            QUIZ_GRADEBOOK_CLASS_SECTION,
            QUIZ_GRADEBOOK_COURSE,
            ASSIGNMENT_TEACHING_OVERVIEW,
            ADMIN_REPORT_SUMMARY,
            CLASS_REPORT_OVERVIEW,
            CLASS_ASSIGNMENT_REPORT
    );

    private CacheNames() {
    }
}
