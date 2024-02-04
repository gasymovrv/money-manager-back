package ru.rgasymov.moneymanager.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rgasymov.moneymanager.constant.CacheNames;
import ru.rgasymov.moneymanager.domain.dto.response.UserResponseDto;
import ru.rgasymov.moneymanager.domain.entity.User;
import ru.rgasymov.moneymanager.exception.ResourceNotFoundException;
import ru.rgasymov.moneymanager.mapper.UserMapper;
import ru.rgasymov.moneymanager.repository.ExpenseCategoryRepository;
import ru.rgasymov.moneymanager.repository.IncomeCategoryRepository;
import ru.rgasymov.moneymanager.repository.UserRepository;
import ru.rgasymov.moneymanager.security.UserPrincipal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  private final UserMapper userMapper;

  private final IncomeCategoryRepository incomeCategoryRepository;

  private final ExpenseCategoryRepository expenseCategoryRepository;

  @Cacheable(cacheNames = CacheNames.USERS)
  @Transactional(readOnly = true)
  public UserDetails loadUserByIdAsUserDetails(String id) {
    var user = getUser(id);
    return UserPrincipal.create(user);
  }

  public User getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var principal = (UserPrincipal) authentication.getPrincipal();
    return principal.getBusinessUser();
  }

  public UserResponseDto getCurrentUserAsDto() {
    var currentUser = getCurrentUser();
    var currentAccountId = currentUser.getCurrentAccount().getId();
    var resp = userMapper.toDto(currentUser);

    resp.getCurrentAccount().setDraft(
        !incomeCategoryRepository.existsByAccountId(currentAccountId)
            && !expenseCategoryRepository.existsByAccountId(currentAccountId)
    );
    return resp;
  }

  @Transactional(readOnly = true)
  public Optional<User> findById(String id) {
    return userRepository.findById(id);
  }

  @CacheEvict(cacheNames = {CacheNames.USERS}, allEntries = true)
  @Transactional
  public User save(User user) {
    return userRepository.save(user);
  }

  private User getUser(String id) {
    return userRepository.findById(id).orElseThrow(
        () -> new ResourceNotFoundException("User", "id", id)
    );
  }
}
