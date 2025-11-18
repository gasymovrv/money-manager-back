package ru.rgasymov.moneymanager.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class SpecUtilsTest {

  @Test
  void andOptionally_shouldAddSpecification_whenStringArgumentIsNotBlank() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);
    var combinedSpec = mock(Specification.class);
    when(sourceSpec.and(additionalSpec)).thenReturn(combinedSpec);

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, "test");

    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(combinedSpec);
  }

  @Test
  void andOptionally_shouldNotAddSpecification_whenStringArgumentIsBlank() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, "   ");

    assertThat(result).isEqualTo(sourceSpec);
  }

  @Test
  void andOptionally_shouldNotAddSpecification_whenStringArgumentIsNull() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, (String) null);

    assertThat(result).isEqualTo(sourceSpec);
  }

  @Test
  void andOptionally_shouldAddSpecification_whenCollectionIsNotEmpty() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);
    var combinedSpec = mock(Specification.class);
    when(sourceSpec.and(additionalSpec)).thenReturn(combinedSpec);
    var list = List.of("item1", "item2");

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, list);

    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(combinedSpec);
  }

  @Test
  void andOptionally_shouldNotAddSpecification_whenCollectionIsEmpty() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);
    var list = new ArrayList<String>();

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, list);

    assertThat(result).isEqualTo(sourceSpec);
  }

  @Test
  void andOptionally_shouldNotAddSpecification_whenCollectionIsNull() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, (List<String>) null);

    assertThat(result).isEqualTo(sourceSpec);
  }

  @Test
  void andOptionally_shouldAddSpecification_whenObjectIsNotNull() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);
    var combinedSpec = mock(Specification.class);
    when(sourceSpec.and(additionalSpec)).thenReturn(combinedSpec);
    var arg = Integer.valueOf(42);

    var result = SpecUtils.andOptionally(sourceSpec, a -> additionalSpec, arg);

    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(combinedSpec);
  }

  @Test
  void andOptionally_shouldNotAddSpecification_whenObjectIsNull() {
    Specification<String> sourceSpec = mock(Specification.class);
    Specification<String> additionalSpec = mock(Specification.class);

    var result = SpecUtils.andOptionally(sourceSpec, arg -> additionalSpec, (Integer) null);

    assertThat(result).isEqualTo(sourceSpec);
  }

  @Test
  void prepareSearchPattern_shouldWrapTextWithWildcards() {
    var result = SpecUtils.prepareSearchPattern("test");

    assertThat(result).isEqualTo("%test%");
  }

  @Test
  void prepareSearchPattern_shouldConvertToLowerCase() {
    var result = SpecUtils.prepareSearchPattern("TEST");

    assertThat(result).isEqualTo("%test%");
  }

  @Test
  void prepareSearchPattern_shouldReplaceSpacesWithWildcards() {
    var result = SpecUtils.prepareSearchPattern("hello world");

    assertThat(result).isEqualTo("%hello%world%");
  }

  @Test
  void prepareSearchPattern_shouldReplaceCommasWithWildcards() {
    var result = SpecUtils.prepareSearchPattern("one,two,three");

    assertThat(result).isEqualTo("%one%two%three%");
  }

  @Test
  void prepareSearchPattern_shouldReplaceMultipleSpacesAndCommas() {
    var result = SpecUtils.prepareSearchPattern("test  value,  another");

    assertThat(result).isEqualTo("%test%value%another%");
  }
}
