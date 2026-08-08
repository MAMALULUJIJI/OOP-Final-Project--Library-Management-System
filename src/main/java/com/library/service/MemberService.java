package com.library.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Member;
import com.library.domain.MemberStatus;
import com.library.domain.Role;
import com.library.repository.MemberRepository;

/**
 * Member management rules (proposal 2.2). Registration is done by a
 * librarian at the desk; members do not self-register. New members always
 * start as ACTIVE with role MEMBER — the form cannot say otherwise.
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository members, PasswordEncoder passwordEncoder) {
        this.members = members;
        this.passwordEncoder = passwordEncoder;
    }

    /** The member register, alphabetically by name. */
    public List<Member> list() {
        return members.findAllByOrderByNameAsc();
    }

    public Member getById(Long id) {
        return members.findById(id).orElseThrow(() -> new MemberNotFoundException(id));
    }

    /**
     * Issue a library card. Email is the login identifier: it is normalized
     * to lower case so K.Agrawal@ and k.agrawal@ cannot become two accounts,
     * and the password is stored only as a BCrypt hash.
     */
    @Transactional
    public Member register(MemberForm form) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (members.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Member member = new Member();
        member.setName(form.getName().trim());
        member.setEmail(email);
        member.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        member.setRole(Role.MEMBER);
        member.setJoinDate(LocalDate.now());
        member.setStatus(MemberStatus.ACTIVE);
        return members.save(member);
    }

    /** Suspended members keep their record and history but cannot borrow. */
    @Transactional
    public Member suspend(Long id) {
        Member member = getById(id);
        member.setStatus(MemberStatus.SUSPENDED);
        return member;
    }

    @Transactional
    public Member reactivate(Long id) {
        Member member = getById(id);
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }
}
