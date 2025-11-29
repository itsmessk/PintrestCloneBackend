package com.infy.pinterest.service;

import com.infy.pinterest.dto.*;
import com.infy.pinterest.entity.User;
import com.infy.pinterest.exception.*;
import com.infy.pinterest.repository.*;
import com.infy.pinterest.utility.FileUploadService;
import com.infy.pinterest.utility.JwtUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private FileUploadService fileUploadService;




    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_TIME_DURATION = 60; // seconds


    public UserProfileDTO getUserProfile(String userId, String viewerId) {
        log.info("Getting profile for user: {} viewed by: {}", userId, viewerId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserProfileDTO profile = modelMapper.map(user, UserProfileDTO.class);

        // Get statistics
        ProfileStatsDTO stats = getProfileStats(userId);
        profile.setStats(stats);

        // If viewing another user's profile, get social relationship info
        if (viewerId != null && !userId.equals(viewerId)) {
            profile.setIsFollowing(followRepository.existsByFollowerIdAndFollowingId(viewerId,
                    userId));
            profile.setIsFollower(followRepository.existsByFollowerIdAndFollowingId(userId,
                    viewerId));
            profile.setIsBlocked(blockedUserRepository.existsByBlockerIdAndBlockedId(viewerId,
                    userId));
        }

        return profile;
    }
    /**
     * Get profile statistics
     */
    public ProfileStatsDTO getProfileStats(String userId) {
        log.info("Getting profile statistics for user: {}", userId);

        ProfileStatsDTO stats = new ProfileStatsDTO();

        // Count pins and boards
        stats.setTotalPins(pinRepository.countByUserId(userId));
        stats.setTotalBoards(boardRepository.countByUserId(userId));

        // Count followers and following
        stats.setFollowers(followRepository.countByFollowingId(userId));
        stats.setFollowing(followRepository.countByFollowerId(userId));

        // Get total saves and likes across all pins
        Long totalSaves = pinRepository.findByUserId(userId).stream()
                .mapToLong(pin -> pin.getSaveCount())
                .sum();
        Long totalLikes = pinRepository.findByUserId(userId).stream()
                .mapToLong(pin -> pin.getLikeCount())
                .sum();

        stats.setTotalPinSaves(totalSaves);
        stats.setTotalPinLikes(totalLikes);

        return stats;
    }
    /**
     * Update user profile
     */
    public UserResponseDTO updateProfile(String userId, UserUpdateDTO updateDTO) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update fields if provided
        if (updateDTO.getFullName() != null) {
            user.setFullName(updateDTO.getFullName());
        }
        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }
        if (updateDTO.getMobileNumber() != null) {
            user.setMobileNumber(updateDTO.getMobileNumber());
        }
        if (updateDTO.getAccountType() != null) {
            user.setAccountType(User.AccountType.valueOf(updateDTO.getAccountType()));
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", userId);

        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }
    /**
     * Upload profile picture
     */
    public UserResponseDTO uploadProfilePicture(String userId, MultipartFile file) {
        log.info("Uploading profile picture for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Upload image
        String imageUrl = fileUploadService.uploadImage(file);

        // Update user profile picture
        user.setProfilePictureUrl(imageUrl);
        User updatedUser = userRepository.save(user);

        log.info("Profile picture updated successfully for user: {}", userId);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }
    /**
     * Delete profile picture
     */
    public UserResponseDTO deleteProfilePicture(String userId) {
        log.info("Deleting profile picture for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setProfilePictureUrl(null);
        User updatedUser = userRepository.save(user);

        log.info("Profile picture deleted successfully for user: {}", userId);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }
    /**
     * Deactivate account
     */
    public void deactivateAccount(String userId) {
        log.info("Deactivating account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("Account deactivated successfully for user: {}", userId);
    }
    /**
     * Reactivate account
     */
    public void reactivateAccount(String userId) {
        log.info("Reactivating account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " +
                        userId));
        user.setIsActive(true);
        userRepository.save(user);

        log.info("Account reactivated successfully for user: {}", userId);
    }
    /**
     * Search users by username or full name
     */
    public PaginatedResponse<UserResponseDTO> searchUsers(String keyword, int page, int size) {
        log.info("Searching users with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<User> userPage = userRepository.searchUsers(keyword, pageable);

        List<UserResponseDTO> users = userPage.getContent().stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                userPage.getNumber(),
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.getSize(),
                userPage.hasNext(),
                userPage.hasPrevious()
        );

        return new PaginatedResponse<>(users, pagination);
    }




    /** * Register a new user
     */
    public UserResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        log.info("Registering new user with email: {}", registrationDTO.getEmail());

        // Check if passwords match
        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setAccountType(User.AccountType.personal);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    /**
     * Login user with circuit breaker
     */
    @CircuitBreaker(name = "loginCircuitBreaker", fallbackMethod = "loginFallback")
    public LoginResponseDTO loginUser(UserLoginDTO loginDTO) {
        log.info("Login attempt for email: {}", loginDTO.getEmail());

        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

                        // Check if account is locked
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            if (user.getLastFailedLoginAt() != null) {
                long secondsSinceLastAttempt = Duration.between( user.getLastFailedLoginAt(),
                        LocalDateTime.now()
                ).getSeconds();

                if (secondsSinceLastAttempt < LOCK_TIME_DURATION) {
                    int retryAfter = (int) (LOCK_TIME_DURATION - secondsSinceLastAttempt);
                    throw new AccountLockedException(
                            "Too many failed login attempts. Please try again after " +
                                    retryAfter + " seconds",
                            retryAfter
                    );
                } else {
                    // Reset failed attempts after lock duration
                    user.setFailedLoginAttempts(0);
                    user.setLastFailedLoginAt(null);
                    userRepository.save(user);
                }
            }
        }

        // Verify password
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            user.setLastFailedLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.warn("Failed login attempt {} for user: {}",
                    user.getFailedLoginAttempts(), user.getEmail());

            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginAt(null);
            userRepository.save(user);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(),
                user.getEmail());

        log.info("User logged in successfully: {}", user.getUserId());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail()); response.setToken(token);
        response.setExpiresIn(3600); // 1 hour

        return response;
    }

    /**
     * Fallback method for circuit breaker
     */
    public LoginResponseDTO loginFallback(UserLoginDTO loginDTO, Exception ex) {
        log.error("Circuit breaker activated for login", ex);
        throw new AccountLockedException(
                "Too many failed login attempts. Please try again after 60 seconds",
                60
        );
    }

    /**
     * Request password reset - Generate OTP
     */
    public void requestPasswordReset(PasswordResetRequestDTO requestDTO) {
        log.info("Password reset requested for email: {}", requestDTO.getEmail());

        User user = userRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " +
                        requestDTO.getEmail()));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // TODO: Send OTP to user's mobile number via SMS service
        // For now, just log it (in production, use SMS gateway)
        log.info("Generated OTP for {}: {}", user.getEmail(), otp);
        log.info("Send OTP to mobile: {}", maskPhoneNumber(user.getMobileNumber()));

        // In a real application, save OTP to password_reset_tokens table
    }

    /**
     * Verify OTP and reset password
     */
    public void verifyAndResetPassword(PasswordResetVerifyDTO verifyDTO) {
        log.info("Verifying OTP and resetting password for email: {}", verifyDTO.getEmail());

        // Check if passwords match
        if (!verifyDTO.getNewPassword().equals(verifyDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        User user = userRepository.findByEmail(verifyDTO.getEmail()) .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " +
                verifyDTO.getEmail()));

        // TODO: Verify OTP from database (password_reset_tokens table)
        // For now, accepting any OTP for demo purposes

        // Update password
        user.setPasswordHash(passwordEncoder.encode(verifyDTO.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getUserId());
    }

    /**
     * Get user by ID
     */
    public UserResponseDTO getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " +
                        userId));

        return modelMapper.map(user, UserResponseDTO.class);
    }

    /**
     * Helper method to mask phone number
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }
        return phone.substring(0, phone.length() - 4).replaceAll(".", "*") +
                phone.substring(phone.length() - 4);
    }
}
