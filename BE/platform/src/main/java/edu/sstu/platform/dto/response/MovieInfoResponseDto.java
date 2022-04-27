package edu.sstu.platform.dto.response;

import edu.sstu.platform.model.AgeRating;
import edu.sstu.platform.model.Language;
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
public class MovieInfoResponseDto extends MovieResponseDto {

  private byte[] background;
  private String pageColor1;
  private String pageColor2;
  private String tagline;
  private String description;
  private AgeRating ageRating;
  private String trailerUrl;
  private int oscars;
  private LocalTime duration;
  private Set<String> originCountries;
  private Set<Language> audioLanguages;
  private Set<Language> subtitleLanguages;
  private Set<GenreResponseDto> genres;
  private Set<ExternalAggregatorInfoResponseDto> externalAggregatorInfos;
}