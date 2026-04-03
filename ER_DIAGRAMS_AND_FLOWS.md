# Entity Relationship Diagrams & Data Flow Analysis

## Overview

This document contains detailed ER diagrams and data flow visualizations for the LMS curriculum template, question bank, and assignment systems.

---

## 1. Complete Database Schema Relationships

### Core Tables for Curriculum Management

```
SUBJECTS
├─ Many CURRICULUMTEMPLATEs
│  └─ Many CURRICULUM_VERSIONs
│     ├─ Many CHAPTER_TEMPLATEs
│     │  └─ Many CONTENT_ITEM_TEMPLATEs
│     │     ├─ LESSONs (optional reference)
│     │     ├─ QUIZZEs (optional reference)
│     │     └─ ASSIGNMENTs (optional reference)
│     │
│     ├─ Many QUESTION_BANKs (CURRICULUM scope)
│     │  └─ Many BANK_QUESTIONs
│     │
│     └─ Many CLASS_SECTIONs (uses this version)
│        ├─ Many CLASS_CHAPTERs
│        │  └─ Many CLASS_CONTENT_ITEMs
│        │     ├─ override_lesson_id
│        │     ├─ override_quiz_id
│        │     └─ override_assignment_id
│        │
│        ├─ Many QUESTION_BANKs (CLASS scope)
│        │  └─ Many BANK_QUESTIONs
│        │
│        └─ Many ENROLLMENTs
```

### Question Bank Scope Resolution

```
QUESTION_BANKS
├─ SUBJECT Scope
│  ├─ subject_id ──────► SUBJECTS
│  ├─ curriculum_version_id = NULL
│  ├─ class_section_id = NULL
│  └─ BANK_QUESTIONs
│     ├─ Shared across multiple curricula
│     ├─ Shared across multiple classes
│     └─ Reused in many quizzes
│
├─ CURRICULUM Scope
│  ├─ curriculum_version_id ──────► CURRICULUM_VERSIONs
│  ├─ subject_id = NULL
│  ├─ class_section_id = NULL
│  └─ BANK_QUESTIONs
│     ├─ Specific to curriculum
│     ├─ Shared across classes using same curriculum
│     └─ Reused in many quizzes within curriculum
│
└─ CLASS Scope
   ├─ class_section_id ──────► CLASS_SECTIONs
   ├─ subject_id = NULL
   ├─ curriculum_version_id = NULL
   └─ BANK_QUESTIONs
      ├─ Only in this class
      ├─ Created by teacher for this class
      └─ Reused in multiple quizzes within class
```

---

## 2. Data Cardinality & Relationships

### Template Hierarchy (Parent-Child)

```
┌─ CurriculumTemplate (1) ─────────────────────────────────┐
│                                                           │
│  name: "Calculus Curriculum"                             │
│  subject_id: 1                                           │
│  is_default: false                                       │
│  is_deleted: false                                       │
└──────────────┬──────────────────────────────────────────┘
               │
        (1:M relationship)
        │
        ▼
┌─ CurriculumVersion (1..N) ──────────────────────────────┐
│  id   │ template_id │ version_no │ status     │ based_on │
│───────┼─────────────┼────────────┼────────────┼──────────│
│ 100   │ 1           │ 1          │ DRAFT      │ NULL     │
│ 101   │ 1           │ 2          │ PUBLISHED  │ 100      │
│ 102   │ 1           │ 3          │ ARCHIVED   │ 101      │
└──────────────┬──────────────────────────────────────────┘
               │
        (1:M relationship)
        │
        ├──────────────────────┬────────────────────┐
        │                      │                    │
        ▼                      ▼                    ▼
┌─ ChapterTemplate  ┌─ QuestionBank    ┌─ ClassSection
│ id: 1001         │ id: 2001         │ id: 3001
│ title: "Ch 1"    │ scope: CURRICULUM│ class_code: A-2025
│ orderIndex: 1    │ name: "Chapter 1"│ status: PUBLISHED
│ order: 1         │ questions: 45    │ teacher_id: 5
└────────┬─────────└─ BankQuestion[]  └────────┬──────────
         │             (45 questions)          │
         │                                      │
    (1:M)│                              (1:M) relationship
         │                                      │
         ▼                                      ▼
┌─ ContentItemTemplate ──────────────────┬─ ClassChapter
│ id: 1011                               │ id: 3011
│ item_type: QUIZ                        │ chapter_template_id: 1001
│ quiz_id: 501                           │ title_override: NULL
│ orderIndex: 1                          │ orderIndex: 1
└────────────────────────────────────────┴────────┬──────
                                                   │
                                            (1:M) relationship
                                                   │
                                                   ▼
                                            ┌─ ClassContentItem
                                            │ id: 3111
                                            │ item_type: QUIZ
                                            │ override_quiz_id: NULL (use template)
                                            │ orderIndex: 1
                                            └─
```

---

## 3. Quiz Question Flow (Inheritance Pattern)

### Question Bank → Quiz → Student Attempt

```
Step 1: SETUP (Question Bank Creation)
┌────────────────────────────────────────────────────┐
│ Teacher creates questions in QUESTION_BANK         │
│                                                    │
│ QUESTION_BANK (scope=CURRICULUM)                   │
│ ├─ BANK_QUESTION #1                               │
│ │  ├─ content: "What is the derivative of x²?"   │
│ │  ├─ type: MULTIPLE_CHOICE                       │
│ │  ├─ difficulty: MEDIUM                          │
│ │  ├─ defaultPoints: 5                            │
│ │  └─ BANK_QUESTION_OPTION (4 options)            │
│ │     ├─ "2x" (isCorrect: true)                   │
│ │     ├─ "2" (isCorrect: false)                   │
│ │     ├─ "x²/2" (isCorrect: false)                │
│ │     └─ "x" (isCorrect: false)                   │
│ │                                                  │
│ └─ BANK_QUESTION #2..#45                          │
└────────────────────────────────────────────────────┘

Step 2: QUIZ CONFIGURATION
┌────────────────────────────────────────────────────┐
│ Teacher creates QUIZ and links to QUESTION_BANK    │
│                                                    │
│ QUIZ (id=501)                                      │
│ ├─ title: "Chapter 1 Assessment"                  │
│ ├─ minPassScore: 60                               │
│ ├─ maxAttempts: 2                                 │
│ │                                                  │
│ └─ QUIZ_BANK_SOURCE #1                            │
│    ├─ selectionMode: RANDOM                       │
│    ├─ questionCount: 10                           │
│    ├─ difficultyLevel: MEDIUM                     │
│    └─ question_bank_id: 2001                      │
│                                                    │
│ └─ QUIZ_BANK_SOURCE #2                            │
│    ├─ selectionMode: ALL_MATCHED                  │
│    ├─ difficultyLevel: HARD                       │
│    ├─ tag_id: 100 (e.g., "Integration")          │
│    └─ question_bank_id: 2001                      │
│                                                    │
│ After configuration, system generates:            │
│ QUIZ_QUESTION[] (auto-populated)                  │
│ ├─ QUIZ_QUESTION #1                               │
│ │  ├─ content: "What is the derivative of x²?"   │
│ │  ├─ source_bank_question_id: 1 (ref to BQ#1)  │
│ │  ├─ type: MULTIPLE_CHOICE                       │
│ │  ├─ points: 5                                   │
│ │  └─ QUIZ_ANSWER[] (from bank options)           │
│ │     ├─ QUIZ_ANSWER "2x" (isCorrect: true)      │
│ │     ├─ QUIZ_ANSWER "2" (isCorrect: false)      │
│ │     └─ ... (other options)                      │
│ │                                                  │
│ └─ QUIZ_QUESTION #2..#(varies)                    │
└────────────────────────────────────────────────────┘

Step 3: STUDENT TAKES QUIZ
┌────────────────────────────────────────────────────┐
│ Student attempts quiz and submits answers          │
│                                                    │
│ QUIZ_ATTEMPT (id=5001)                            │
│ ├─ quiz_id: 501                                   │
│ ├─ student_id: 10                                 │
│ ├─ startTime: 2025-03-28 14:00                   │
│ ├─ completedTime: 2025-03-28 14:30               │
│ ├─ status: COMPLETED                              │
│ ├─ correctAnswers: 8                              │
│ ├─ totalQuestions: 10                             │
│ ├─ grade: 80                                      │
│ └─ isPassed: true (80 >= 60)                      │
│                                                    │
│ └─ QUIZ_ATTEMPT_ANSWER[] (one per question)       │
│    ├─ QUIZ_ATTEMPT_ANSWER #1                      │
│    │  ├─ question_id: (ref to QUIZ_QUESTION #1) │
│    │  ├─ textAnswer: NULL                         │
│    │  ├─ selectedAnswers: ["2x"] (MCQ selection) │
│    │  ├─ isCorrect: true                         │
│    │  └─ completedAt: 2025-03-28 14:05          │
│    │                                              │
│    ├─ QUIZ_ATTEMPT_ANSWER #2..#10                │
│    │  (student's responses to each question)      │
│    │                                              │
│    └─ (Auto-graded based on QUIZ_ANSWER)         │
└────────────────────────────────────────────────────┘
```

---

## 4. Assignment System Flow (TO BE IMPLEMENTED)

### Current Missing Structure

```
MISSING: Assignment → Attempt → Submission

Proposed Structure:

Step 1: TEACHER CREATES ASSIGNMENT
┌────────────────────────────────────────┐
│ ASSIGNMENT (id=401)                    │
├─ title: "Project 1"                   │
├─ description: "Build a calculator"    │
├─ instruction: "Step 1: Design..."     │
├─ maxScore: 100                         │
├─ dueAt: 2025-04-15 23:59              │
├─ allowLateSubmission: true             │
├─ is_deleted: false                     │
└─ (MISSING: classSection link)          │
   (MISSING: need CLASS_CONTENT_ITEM)    │

Step 2: STUDENT CREATES ATTEMPT
┌────────────────────────────────────────┐
│ ASSIGNMENT_ATTEMPT (id=4001)           │
├─ assignment_id: 401                    │
├─ student_id: 10                        │
├─ status: DRAFT (can save multiple)     │
├─ isLate: false (submitted before due)  │
├─ submittedAt: 2025-04-15 20:30        │
├─ score: NULL (not graded yet)          │
├─ feedback: NULL                        │
└─ gradedAt: NULL                        │

Step 3: STUDENT SUBMITS
┌────────────────────────────────────────┐
│ ASSIGNMENT_SUBMISSION (id=4101)        │
├─ attempt_id: 4001                      │
├─ submissionText: "Here's my solution..." │
├─ fileUrl: "https://cdn/project1.zip"   │
├─ cloudinaryId: "xyz-abc-123"           │
├─ submittedAt: 2025-04-15 20:30        │
├─ isDraft: false                        │
│                                         │
│ (Status: DRAFT → SUBMITTED)            │
└─ attempt.status changed to SUBMITTED   │

Step 4: TEACHER GRADES
┌────────────────────────────────────────┐
│ ASSIGNMENT_ATTEMPT (updated)           │
├─ status: GRADED                        │
├─ score: 85                             │
├─ feedback: "Good work, but..."         │
├─ gradedAt: 2025-04-16 10:00           │
└─ (Linked to ASSIGNMENT_SUBMISSION)     │

Step 5: STUDENT VIEWS RESULT
┌────────────────────────────────────────┐
│ Display ASSIGNMENT_ATTEMPT #4001       │
├─ Score: 85/100 (B)                     │
├─ Feedback: "Good work, but..."         │
├─ Submitted at: 2025-04-15 20:30       │
├─ Graded at: 2025-04-16 10:00          │
└─ Can resubmit if allowed               │
```

---

## 5. Template Customization Scenarios

### Scenario 1: Using Template As-Is

```
Teacher A uses CurriculumVersion as template:

CurriculumVersion v2 (PUBLISHED) ──(copy)──► ClassSection "A-2025"
│                                                  │
├─ ChapterTemplate "Ch 1"        ─────────────► ClassChapter
│  ├─ ContentItemTemplate "Lesson 1"  ────────► ClassContentItem (no override)
│  ├─ ContentItemTemplate "Quiz 1"    ────────► ClassContentItem (no override)
│  └─ ContentItemTemplate "Assignment 1" ────► ClassContentItem (no override)
│
└─ QuestionBank (Curriculum scope) ───────────► Used as-is for quiz

Result: Students see exact template content
```

### Scenario 2: Customized Template

```
Teacher B customizes while using template:

CurriculumVersion v2 (PUBLISHED) ──(copy)──► ClassSection "B-2025"
│                                                  │
├─ ChapterTemplate "Ch 1"        ──────────────► ClassChapter
│  │ titleOverride: "Chapter 1: Derivatives"   (OVERRIDE)
│  │ descriptionOverride: "Learn calculus..."  (OVERRIDE)
│  │
│  ├─ ContentItemTemplate "Quiz 1" ───────────► ClassContentItem
│  │  │ (Template points to QUIZ #501)
│  │  │
│  │  └─ override_quiz_id = 502  (CREATE NEW QUIZ with different questions)
│  │      └─ QUIZ #502 (Teacher's custom quiz for this class)
│  │         └─ QUIZ_BANK_SOURCE pointing to CLASS-scoped bank
│  │
│  └─ ContentItemTemplate "Assignment 1" ────► ClassContentItem
│     │ (Template points to ASSIGNMENT #401)
│     │
│     └─ override_assignment_id = 402  (MODIFY OR CREATE NEW)
│         └─ ASSIGNMENT #402 (Customized due date/score)
│
└─ QuestionBank (CLASS scope) created ────────► Used for quizzes in this class
   Name: "B-2025 Chapter 1 Questions"

Result: Students see teacher B's customized content
```

### Scenario 3: Flexible Override

```
ClassChapter has these fields for flexibility:

┌─ ClassChapter ─────────────────────────────────┐
│ classChapter_id: 3011                         │
│ chapter_template_id: 1001                     │
│ title_override: "Advanced Derivatives"        │
│ description_override: "Deep dive into..."     │
│ orderIndex: 1                                 │
│ isHidden: false                               │
└─ Can override title without affecting template
   Can override description without affecting template
   Can show/hide entire chapter
   Can reorder chapters for this class

┌─ ClassContentItem ─────────────────────────────┐
│ classContentItem_id: 3111                     │
│ contentItemTemplate_id: 1011                  │
│ title_override: "Test Quiz"                   │
│ itemType: QUIZ                                │
│ override_quiz_id: 502 (NULL = use template)   │
│ override_assignment_id: NULL                  │
│ override_lesson_id: NULL                      │
│ isHidden: false                               │
└─ If override_quiz_id is set: use custom quiz
   If override_quiz_id is NULL: use template quiz
   Can override title for this class
   Can hide item from this class
   Can change ordering per class
```

---

## 6. Question Bank Scope Examples

### Example 1: SUBJECT Scope
```
Subject: "Mathematics"
└─ QuestionBank (SUBJECT scope)
   Name: "All Math Questions"
   Scope: SUBJECT
   subject_id: 1
   ├─ BankQuestion: Algebra, Geometry, Calculus...
   └─ Used by:
      ├─ Curriculum A (Calculus)
      ├─ Curriculum B (Statistics)
      └─ Curriculum C (Linear Algebra)

Multiple curricula can share the same question bank.
Teacher can search across subject-level questions.
```

### Example 2: CURRICULUM Scope
```
Subject: "Mathematics"
└─ CurriculumTemplate: "Calculus Curriculum"
   └─ CurriculumVersion: v2 (PUBLISHED)
      └─ QuestionBank (CURRICULUM scope)
         Name: "Calculus Level Concepts"
         Scope: CURRICULUM
         curriculum_version_id: 101
         ├─ BankQuestion: Derivatives, Integrals, Limits...
         └─ Used by:
            ├─ Class A-2025 (Spring Calc)
            ├─ Class A-2026 (Spring Calc)
            └─ Class B-2025 (Fall Calc)

Only Calculus curriculum can use these questions.
Multiple classes using same curriculum share same bank.
Teacher customizes per curriculum version.
```

### Example 3: CLASS Scope
```
Subject: "Mathematics"
└─ CurriculumVersion: v2 (PUBLISHED)
   └─ ClassSection: "A-2025 Advanced Calculus"
      └─ QuestionBank (CLASS scope)
         Name: "A-2025 Chapter 1 Questions"
         Scope: CLASS
         class_section_id: 3001
         ├─ BankQuestion: Only for this class
         └─ NOT shared with other classes

Only this class can use these questions.
Teacher A customized these for their specific class.
Other teachers using same curriculum don't see these.
```

---

## 7. Query Patterns for Complex Operations

### Get all content for a class (respecting overrides)

```sql
SELECT 
    cci.id as content_item_id,
    cci.title_override,
    cci.item_type,
    -- If override exists, use it; else use template
    COALESCE(cci.override_lesson_id, cit.lesson_id) as actual_lesson_id,
    COALESCE(cci.override_quiz_id, cit.quiz_id) as actual_quiz_id,
    COALESCE(cci.override_assignment_id, cit.assignment_id) as actual_assignment_id
FROM class_content_items cci
JOIN class_chapters cc ON cci.class_chapter_id = cc.id
JOIN chapter_templates cit ON cc.chapter_template_id = cit.id
WHERE cc.class_section_id = ?
    AND cci.is_deleted = false
    AND cci.is_hidden = false
ORDER BY cc.order_index, cci.order_index;
```

### Get questions for a quiz (from bank or direct)

```sql
-- If using QuizBankSource
SELECT DISTINCT bq.*
FROM bank_questions bq
JOIN question_banks qb ON bq.question_bank_id = qb.id
JOIN quiz_bank_sources qbs ON qbs.question_bank_id = qb.id
WHERE qbs.quiz_id = ? 
    AND bq.is_deleted = false
    AND (
        -- Filter by difficulty if specified
        (qbs.difficulty_level IS NULL) OR 
        (bq.difficulty_level = qbs.difficulty_level)
    )
    AND (
        -- Filter by tag if specified
        (qbs.tag_id IS NULL) OR
        EXISTS (
            SELECT 1 FROM bank_question_tags bqt
            WHERE bqt.bank_question_id = bq.id
            AND bqt.tag_id = qbs.tag_id
        )
    )
LIMIT qbs.question_count;
```

### Find unused question bank questions (for cleanup)

```sql
SELECT bq.*
FROM bank_questions bq
WHERE bq.question_bank_id = ?
    AND bq.is_deleted = false
    AND NOT EXISTS (
        SELECT 1 FROM quiz_questions qq
        WHERE qq.source_bank_question_id = bq.id
    );
```

---

## 8. Data Consistency Rules (Validation)

### Rules that must be enforced in service layer:

```
1. CURRICULUM TEMPLATE RULES:
   ✓ CurriculumVersion.basedOnVersion must be older version
   ✓ Cannot delete version while classes are using it
   ✓ Versioning must be sequential (v1, v2, v3...)
   ✓ Only one version can be PUBLISHED per template
   
2. CHAPTER TEMPLATE RULES:
   ✓ orderIndex must be sequential within version
   ✓ Cannot delete chapter if classes reference it
   
3. QUESTION BANK SCOPE RULES:
   ✓ SUBJECT scope: subject_id NOT NULL, others NULL
   ✓ CURRICULUM scope: curriculum_version_id NOT NULL, others NULL
   ✓ CLASS scope: class_section_id NOT NULL, others NULL
   ✓ Exactly ONE scope field must be filled
   
4. QUIZ-BANK SOURCE RULES:
   ✓ If selectionMode=MANUAL, questionCount should be ignored
   ✓ If selectionMode=RANDOM, questionCount required
   ✓ If selectionMode=ALL_MATCHED, all matching questions selected
   ✓ Cannot create quiz without questions
   
5. ASSIGNMENT RULES:
   ✓ dueAt must be in future (when created)
   ✓ maxScore must be > 0
   ✓ Cannot grade attempt with score > maxScore
   ✓ Only one SUBMITTED attempt per student per assignment
   
6. STUDENT PROGRESS RULES:
   ✓ Can view content only if enrolled in class
   ✓ Can submit after due date only if allowLateSubmission=true
   ✓ Late flag must be set based on dueAt vs submittedAt
```

---

## 9. Performance Considerations

### Indexes to Add

```sql
-- Curriculum & Template Lookups
CREATE INDEX idx_curriculum_template_subject 
ON curriculum_templates(subject_id, is_deleted);

CREATE INDEX idx_curriculum_version_template_status 
ON curriculum_versions(template_id, status, is_deleted);

CREATE INDEX idx_chapter_template_version_order 
ON chapter_templates(curriculum_version_id, order_index);

-- Question Bank Lookups
CREATE INDEX idx_question_bank_scope_subject 
ON question_banks(scope_type, subject_id, is_deleted);

CREATE INDEX idx_question_bank_scope_curriculum 
ON question_banks(scope_type, curriculum_version_id, is_deleted);

CREATE INDEX idx_question_bank_scope_class 
ON question_banks(scope_type, class_section_id, is_deleted);

CREATE INDEX idx_bank_question_difficulty 
ON bank_questions(question_bank_id, difficulty_level, is_deleted);

-- Quiz Lookups
CREATE INDEX idx_quiz_class_section 
ON quiz(class_section_id, is_deleted);

CREATE INDEX idx_quiz_question_quiz 
ON quiz_questions(quiz_id, is_deleted);

-- Class Lookups
CREATE INDEX idx_class_section_curriculum 
ON class_sections(curriculum_version_id, is_deleted);

CREATE INDEX idx_class_chapter_class 
ON class_chapters(class_section_id, order_index, is_deleted);

CREATE INDEX idx_class_content_item_chapter 
ON class_content_items(class_chapter_id, order_index, is_deleted);

-- Assignment Lookups (FUTURE)
CREATE INDEX idx_assignment_attempt_student_assignment 
ON assignment_attempts(student_id, assignment_id, is_deleted);

CREATE INDEX idx_assignment_attempt_status 
ON assignment_attempts(status, is_deleted);
```

### Query Optimization Tips

```
1. Use pagination for large result sets
2. Eager load relationships when needed (use JOIN FETCH)
3. Use projections for read-only queries
4. Cache question bank contents if rarely changed
5. Denormalize question_count in question_banks table
6. Denormalize usage_count in question_banks table
```

---

## Summary: What Needs Implementation

```
┌─────────────────────────────────────────────────────┐
│ PRIORITY 1: CRITICAL (Week 1)                       │
├─────────────────────────────────────────────────────┤
│ ✗ QuestionBankRepository + Service + Controller     │
│ ✗ AssignmentAttempt + AssignmentSubmission entities │
│ ✗ AssignmentService + AssignmentRepository          │
│ ✗ Fix ClassChapter cascade delete                   │
│ ✗ Fix Assignment.classSection link                  │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ PRIORITY 2: HIGH (Week 2)                           │
├─────────────────────────────────────────────────────┤
│ ✗ CurriculumTemplateService + Controller            │
│ ✗ CurriculumVersionService with template-to-class   │
│ ✗ ChapterTemplateService (maybe)                    │
│ ✗ Add database constraints for scope validation     │
│ ✗ Add audit fields (createdBy, versionName)        │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ PRIORITY 3: MEDIUM (Week 3+)                        │
├─────────────────────────────────────────────────────┤
│ ✗ Publishing/Archiving workflow                     │
│ ✗ Question bank filtering & search                  │
│ ✗ Advanced report generation                        │
│ ✗ Performance optimization (caching, indexes)       │
│ ✗ Data migration tools                              │
└─────────────────────────────────────────────────────┘
```


