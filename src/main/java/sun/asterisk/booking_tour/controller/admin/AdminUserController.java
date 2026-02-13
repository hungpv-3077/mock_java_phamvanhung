package sun.asterisk.booking_tour.controller.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sun.asterisk.booking_tour.dto.user.AdminUserListResponse;
import sun.asterisk.booking_tour.dto.user.AdminUserRequest;
import sun.asterisk.booking_tour.enums.UserStatus;
import sun.asterisk.booking_tour.service.AdminUserService;
import sun.asterisk.booking_tour.service.UserCsvExportService;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserCsvExportService userCsvExportService;

    private static List<String> statusNames() {
        return Arrays.stream(UserStatus.values())
                .map(Enum::name)
                .toList();
    }

    private static Map<String, Object> toUserViewModel(AdminUserListResponse user) {
        Map<String, Object> viewModel = new HashMap<>();
        viewModel.put("id", user.getId());
        viewModel.put("firstName", user.getFirstName());
        viewModel.put("lastName", user.getLastName());
        viewModel.put("email", user.getEmail());
        viewModel.put("phone", user.getPhone());
        viewModel.put("dateOfBirth", user.getDateOfBirth());
        viewModel.put("avatarUrl", user.getAvatarUrl());
        viewModel.put("isVerified", user.getIsVerified());
        viewModel.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        viewModel.put("roleName", user.getRoleName());
        return viewModel;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) UserStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());
        try {
            Page<Map<String, Object>> users = adminUserService.getAllUsers(search, status, pageable)
                    .map(AdminUserController::toUserViewModel);
            
            log.info("Fetched {} users, total: {}", users.getNumberOfElements(), users.getTotalElements());
            
            String currentUserEmail = authentication != null ? authentication.getName() : null;
            
            model.addAttribute("users", users);
            model.addAttribute("currentUserEmail", currentUserEmail);
            model.addAttribute("search", search);
            model.addAttribute("selectedStatus", status != null ? status.name() : null);
            model.addAttribute("statuses", statusNames());
            
            return "admin/users";
        } catch (Exception e) {
            log.error("Error fetching users", e);
            model.addAttribute("users", Page.<Map<String, Object>>empty(pageable));
            model.addAttribute("search", search);
            model.addAttribute("selectedStatus", status != null ? status.name() : null);
            model.addAttribute("statuses", statusNames());
            model.addAttribute("error", "Error loading users: " + e.getMessage());
            return "admin/users";
        }
    }

    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new AdminUserRequest());
        model.addAttribute("roles", adminUserService.getAllRoles());
        return "admin/user-form";
    }

    @PostMapping("/add")
    public String addUser(
            @Valid @ModelAttribute("user") AdminUserRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            model.addAttribute("roles", adminUserService.getAllRoles());
            return "admin/user-form";
        }

        try {
            adminUserService.createUser(request);
            redirectAttributes.addFlashAttribute("success", "User created successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", adminUserService.getAllRoles());
            return "admin/user-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        AdminUserListResponse user = adminUserService.getUserById(id);
        
        AdminUserRequest request = AdminUserRequest.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roleId(adminUserService.getRoleIdByUserId(id))
                .build();
        
        model.addAttribute("user", request);
        model.addAttribute("userId", id);
        model.addAttribute("userAvatar", user.getAvatarUrl());
        model.addAttribute("roles", adminUserService.getAllRoles());
        return "admin/user-form";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") AdminUserRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", adminUserService.getAllRoles());
            AdminUserListResponse user = adminUserService.getUserById(id);
            model.addAttribute("userAvatar", user.getAvatarUrl());
            return "admin/user-form";
        }

        try {
            adminUserService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("success", "User updated successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("roles", adminUserService.getAllRoles());
            AdminUserListResponse user = adminUserService.getUserById(id);
            model.addAttribute("userAvatar", user.getAvatarUrl());
            return "admin/user-form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null) {
                redirectAttributes.addFlashAttribute("error", "Authentication required");
                return "redirect:/admin/users";
            }
            
            String currentUserEmail = authentication.getName();
            
            // Double check: prevent self-deletion at controller level
            AdminUserListResponse userToDelete = adminUserService.getUserById(id);
            if (userToDelete.getEmail().equals(currentUserEmail)) {
                redirectAttributes.addFlashAttribute("error", "Not allowed to delete your own account");
                return "redirect:/admin/users";
            }
            
            adminUserService.deleteUser(id, currentUserEmail);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminUserService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("success", "User status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/export/csv")
    public ResponseEntity<?> exportUsersToCsv(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) UserStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            log.info("Starting CSV export - search: {}, status: {}", search, status);

            CompletableFuture<String> futureFilePath = userCsvExportService.exportUsersToCsv(search, status);
            String filePath = futureFilePath.get();

            File file = new File(filePath);
            if (!file.exists()) {
                log.error("CSV file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            log.info("CSV export completed successfully. File size: {} bytes", file.length());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error exporting users to CSV", e);
            return ResponseEntity.internalServerError()
                    .body("Error exporting users: " + e.getMessage());
        }
    }
    
    @GetMapping("/export/csv/all")
    public ResponseEntity<?> exportAllUsersToCsv() {
        return exportUsersToCsv(null, null, null);
    }
}
