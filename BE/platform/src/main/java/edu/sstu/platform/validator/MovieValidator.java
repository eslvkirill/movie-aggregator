package edu.sstu.platform.validator;

import static edu.sstu.platform.config.properties.ValidationProperties.FILE_TYPE;
import static edu.sstu.platform.util.ExceptionUtils.addErrorMessageByField;
import static edu.sstu.platform.util.QuerydslUtils.toDotPath;

import edu.sstu.platform.config.properties.ValidationProperties;
import edu.sstu.platform.dto.request.MovieRequestDto;
import edu.sstu.platform.exception.ValidationException;
import edu.sstu.platform.model.QMovie;
import edu.sstu.platform.repo.MovieRepo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

@Component
@RequiredArgsConstructor
public class MovieValidator {

  private final MovieRepo movieRepo;
  private final ValidationProperties validationProperties;
  private final QMovie qMovie = QMovie.movie;

  @SneakyThrows
  public void validate(MovieRequestDto movieRequestDto, UUID id) {
    var messagesByField = new LinkedMultiValueMap<String, String>();
    var predicate = qMovie.engTitle.equalsIgnoreCase(movieRequestDto.getEngTitle())
        .and(qMovie.year.eq(movieRequestDto.getYear()));

    if (id != null) {
      predicate = predicate.and(qMovie.id.ne(id));
    }

    if (movieRepo.exists(predicate)) {
      addErrorMessageByField(messagesByField, qMovie, validationProperties::getDuplicateMessage);
    }
    if (movieRequestDto.getPoster().isEmpty()) {
      messagesByField.add(toDotPath(qMovie.poster), validationProperties.getEmptyMessage(FILE_TYPE));
    }
    if (movieRequestDto.getBackground().isEmpty()) {
      messagesByField.add(toDotPath(qMovie.background), validationProperties.getEmptyMessage(FILE_TYPE));
    }
    if (!messagesByField.isEmpty()) {
      throw new ValidationException(messagesByField);
    }
  }

  public void validate(MovieRequestDto movieRequestDto) {
    validate(movieRequestDto, UUID.randomUUID());
  }
}
