package com.example.backend.entity.quiz;

import com.example.backend.constant.QuestionBankMemberRole;
import com.example.backend.entity.BaseEntity;
import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "question_bank_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_bank_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionBankMemberRole role;
}
