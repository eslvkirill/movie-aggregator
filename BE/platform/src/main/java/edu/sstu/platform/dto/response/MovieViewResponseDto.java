package edu.sstu.platform.dto.response;

import java.time.LocalTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieViewResponseDto extends MovieResponseDto {

  private byte[] poster;
  private String primaryPageColor;
  private Set<GenreResponseDto> genres;
  private LocalTime duration;
  private Set<PersonViewResponseDto> directors;
  private double totalRating;
}
