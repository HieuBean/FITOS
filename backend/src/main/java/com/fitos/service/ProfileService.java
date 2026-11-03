package com.fitos.service;

import com.fitos.dto.profile.ProfileDtos.CustomerProfile;
import com.fitos.dto.profile.ProfileDtos.PasswordChangeRequest;
import com.fitos.dto.profile.ProfileDtos.ProfileResponse;
import com.fitos.dto.profile.ProfileDtos.ProfileUpdateRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Customer;
import com.fitos.model.User;
import com.fitos.repository.CustomerRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Task #80 (phía API): xem và cập nhật hồ sơ hội viên + đổi mật khẩu. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ProfileResponse get(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Customer customer = customerRepository
                .findByUserIdAndStudioId(user.getId(), principal.getStudioId())
                .orElse(null);
        return toResponse(user, principal, customer);
    }

    @Transactional
    public ProfileResponse update(UserPrincipal principal, ProfileUpdateRequest req) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setFullName(req.fullName().trim());
        if (req.phone() != null) {
            user.setPhone(req.phone().trim());
        }
        userRepository.save(user);

        Customer customer = customerRepository
                .findByUserIdAndStudioId(user.getId(), principal.getStudioId())
                .orElse(null);
        if (customer != null) {
            customer.setFullName(user.getFullName());
            if (req.phone() != null) {
                customer.setPhone(req.phone().trim());
            }
            customer.setDob(req.dob());
            if (req.gender() != null) {
                customer.setGender(req.gender());
            }
            if (req.fitnessGoal() != null) {
                customer.setFitnessGoal(req.fitnessGoal());
            }
            customer.setEmergencyContactName(req.emergencyContactName());
            customer.setEmergencyContactPhone(req.emergencyContactPhone());
            customerRepository.save(customer);
        }
        log.info("Cập nhật hồ sơ cho user {}", user.getId());
        return toResponse(user, principal, customer);
    }

    @Transactional
    public void changePassword(UserPrincipal principal, PasswordChangeRequest req) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        log.info("Đổi mật khẩu cho user {}", user.getId());
    }

    private ProfileResponse toResponse(User user, UserPrincipal principal, Customer customer) {
        List<String> roles = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        CustomerProfile customerProfile = customer == null ? null : new CustomerProfile(
                customer.getId(), customer.getFullName(), customer.getPhone(), customer.getEmail(),
                customer.getDob(), customer.getGender(), customer.getFitnessGoal(),
                customer.getEmergencyContactName(), customer.getEmergencyContactPhone());
        return new ProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                roles, principal.getStudioId(), customerProfile);
    }
}
