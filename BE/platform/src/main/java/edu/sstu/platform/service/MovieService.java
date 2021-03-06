package edu.sstu.platform.service;

import static edu.sstu.platform.model.ExternalAggregator.IMDB;
import static edu.sstu.platform.model.ExternalAggregator.KINOPOISK;
import static edu.sstu.platform.model.ExternalAggregator.METACRITIC;
import static edu.sstu.platform.model.RatingType.TOTAL;
import static edu.sstu.platform.util.ParsingUtils.findFirstMatching;
import static edu.sstu.platform.util.QuerydslUtils.toDotPath;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sstu.platform.dto.request.MoviePageableRequestDto;
import edu.sstu.platform.dto.request.MovieRequestDto;
import edu.sstu.platform.dto.request.MoviePage;
import edu.sstu.platform.dto.response.MovieEditingResponseDto;
import edu.sstu.platform.dto.response.MovieInfoResponseDto;
import edu.sstu.platform.dto.response.MovieResponseDto;
import edu.sstu.platform.dto.response.MovieViewResponseDto;
import edu.sstu.platform.mapper.MovieMapper;
import edu.sstu.platform.model.ExternalAggregator;
import edu.sstu.platform.model.ExternalAggregatorInfo;
import edu.sstu.platform.model.ExternalAggregatorInfo.IdClass;
import edu.sstu.platform.model.Movie;
import edu.sstu.platform.model.QRating;
import edu.sstu.platform.model.QReview;
import edu.sstu.platform.model.Rating;
import edu.sstu.platform.model.RatingType;
import edu.sstu.platform.repo.MovieRepo;
import edu.sstu.platform.repo.RatingRepo;
import edu.sstu.platform.repo.ReviewRepo;
import edu.sstu.platform.validator.MovieValidator;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

  private static final String TITLE_NODE = "Title";
  private static final String RATINGS_NODE = "Ratings";
  private static final String AWARDS_NODE = "Awards";
  private static final String RATING_SOURCE_NODE = "Source";
  private static final String RATING_VALUE_NODE = "Value";
  private static final String KINOPOISK_RATING_NODE = "kp_rating";
  private static final String OSCAR_REG_EXP = "Won (\\d{1,2})";
  private static final String RATING_REG_EXP = "[\\d.]+";
  private static final String ID_REG_EXP = "[\\d]+";

  private final RatingService ratingService;
  private final UserPrincipalService userPrincipalService;
  private final RatingRepo ratingRepo;
  private final MovieRepo movieRepo;
  private final ReviewRepo reviewRepo;
  private final MovieMapper movieMapper;
  private final MovieValidator movieValidator;
  private final RestTemplate restClient;
  private final ObjectMapper objectMapper;
  private final QRating qRating = QRating.rating;
  private final QReview qReview = QReview.review;

  @Value("${app.api.omdb}")
  public String omdbApi;

  @Value("${app.api.kinopoisk}")
  public String kinopoiskApi;

  @Value("${app.url-pattern.metacritic}")
  public String metacriticUrlPattern;

  @Transactional
  public UUID createMovie(MovieRequestDto movieRequestDto) {
    movieValidator.validate(movieRequestDto);

    var movie = movieMapper.toEntity(movieRequestDto);
    populateOmdbData(movie, movieRequestDto);
    populateKinopoiskData(movie, movieRequestDto);

    return movieRepo.save(movie).getId();
  }

  @SneakyThrows
  private void populateOmdbData(Movie movie, MovieRequestDto movieRequestDto) {
    var populatedOmdbApi = format(omdbApi, findFirstMatching(ID_REG_EXP, movieRequestDto.getImdbUrl()));
    var omdbJson = restClient.getForObject(populatedOmdbApi, String.class);

    var omdbRootNode = objectMapper.readTree(omdbJson);
    var awards = omdbRootNode.get(AWARDS_NODE).asText();
    var oscarNumber = Optional.ofNullable(findFirstMatching(OSCAR_REG_EXP, awards))
        .filter(StringUtils::hasText)
        .map(Integer::parseInt)
        .orElse(0);

    movie.setOscars(oscarNumber);

    var ratingNodes = omdbRootNode.get(RATINGS_NODE);
    var externalAggregatorToMovieUrl = new EnumMap<ExternalAggregator, String>(ExternalAggregator.class);
    externalAggregatorToMovieUrl.put(IMDB, movieRequestDto.getImdbUrl());
    externalAggregatorToMovieUrl.put(METACRITIC,
        format(metacriticUrlPattern, omdbRootNode.get(TITLE_NODE).asText().toLowerCase().replace(" ", "-")));

    ratingNodes.forEach(rn -> processRatingNode(rn, movie, externalAggregatorToMovieUrl));
  }

  private void processRatingNode(JsonNode ratingNode, Movie movie,
      EnumMap<ExternalAggregator, String> externalAggregatorToMovieUrl) {
    ofNullable(ExternalAggregator.of(ratingNode.get(RATING_SOURCE_NODE).asText()))
        .map(aggregator -> {
          var aggregatorRating = findFirstMatching(RATING_REG_EXP, ratingNode.get(RATING_VALUE_NODE).asText());
          return ExternalAggregatorInfo.builder()
              .id(IdClass.builder()
                  .aggregator(aggregator)
                  .movie(movie)
                  .build())
              .url(externalAggregatorToMovieUrl.get(aggregator))
              .rating(Float.parseFloat(aggregatorRating))
              .build();
        })
        .ifPresent(movie::addExternalAggregatorInfo);
  }

  @SneakyThrows
  private void populateKinopoiskData(Movie movie, MovieRequestDto movieRequestDto) {
    var kinopoiskId = findFirstMatching(ID_REG_EXP, movieRequestDto.getKinopoiskUrl());
    var kinopoiskJson = restClient.getForObject(String.format(kinopoiskApi, kinopoiskId), String.class);
    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(Objects.requireNonNull(kinopoiskJson))));
    float rating = Float.parseFloat(doc.getElementsByTagName(KINOPOISK_RATING_NODE)
        .item(0).getTextContent());

    movie.addExternalAggregatorInfo(ExternalAggregatorInfo.builder()
        .id(IdClass.builder()
            .aggregator(KINOPOISK)
            .movie(movie)
            .build())
        .url(movieRequestDto.getKinopoiskUrl())
        .rating(rating)
        .build());
  }

  @Transactional
  public void updateMovie(UUID id, MovieRequestDto movieRequestDto) {
    movieValidator.validate(movieRequestDto, id);

    var movie = movieRepo.findMovieById(id)
        .orElseThrow(() -> entityNotFoundException(id));

    movieMapper.update(movieRequestDto, movie);

    var externalAggregatorInfos = movie.getExternalAggregatorInfoAsMap();

    if (!externalAggregatorInfos.get(IMDB).getUrl().equals(movieRequestDto.getImdbUrl())) {
      populateOmdbData(movie, movieRequestDto);
    }
    if (!externalAggregatorInfos.get(KINOPOISK).getUrl().equals(movieRequestDto.getKinopoiskUrl())) {
      populateKinopoiskData(movie, movieRequestDto);
    }
  }

  @Transactional
  public void safeDeleteMovie(UUID id) {
    var updatedCount = movieRepo.safeDeleteById(id);

    if (updatedCount == 0) {
      throw entityNotFoundException(id);
    }
  }

  @Transactional(readOnly = true)
  public MovieResponseDto findMovieById(UUID movieId, MoviePage moviePage) {
    switch (moviePage) {
      case PERSONAL:
        return findMovieInfoById(movieId, userPrincipalService.getCurrentUserOrElse().getId());
      case SAVE:
        return findMovieEditableById(movieId);
      default:
        throw new IllegalArgumentException("Request from unknown movie page");
    }
  }

  private MovieInfoResponseDto findMovieInfoById(UUID movieId, UUID userId) {
    var movie = movieRepo.findMovieById(movieId)
        .orElseThrow(() -> entityNotFoundException(movieId));
    var ratingsByMovieIds = ratingService.findRatingsByMovieIds(List.of(movieId), RatingType.values()).get(movieId);
    var totalRatingCount = ratingRepo.count(qRating.movieId.eq(movieId)
            .and(qRating.ratingType.eq(TOTAL)));
    var userRatings = List.<Rating>of();
    var reviewedByUser = false;

    if (userId != null) {
      var userRatingsPredicate = qRating.movieId.eq(movieId)
          .and(qRating.userId.eq(userId));
      userRatings = ratingRepo.findBy(userRatingsPredicate,
          query -> query.sortBy(Sort.by(toDotPath(qRating.rank))).all());
      reviewedByUser = reviewRepo.exists(qReview.movieId.eq(movieId)
          .and(qReview.userId.eq(userId)));
    }

    var movieInfoResponseDto = movieMapper.toInfoDto(movie, ratingsByMovieIds, userRatings);
    movieInfoResponseDto.setTotalRatingCount(totalRatingCount);
    movieInfoResponseDto.setReviewedByUser(reviewedByUser);

    return movieInfoResponseDto;
  }

  private EntityNotFoundException entityNotFoundException(UUID id) {
    return new EntityNotFoundException("Movie by id: " + id + " doesn't exist");
  }

  private MovieEditingResponseDto findMovieEditableById(UUID id) {
    var movie = movieRepo.findMovieById(id)
        .orElseThrow(() -> entityNotFoundException(id));

    return movieMapper.toEditingDto(movie);
  }

  @Transactional(readOnly = true)
  public Page<MovieViewResponseDto> findMovies(Specification<Movie> movieSpec,
      MoviePageableRequestDto pageableRequest) {
    var movieIdPage = movieRepo.findAllIds(movieSpec, pageableRequest);
    var movieIds = movieIdPage.getContent();
    var movies = movieRepo.findByIdIn(movieIds);
    movies = reorderByIdsOrdering(movieIds, movies);
    var ratingsByMovieId = ratingService.findRatingsByMovieIds(movieIds, TOTAL);
    var movieDtoList = movieMapper.toViewDto(movies, ratingsByMovieId);

    return new PageImpl<>(movieDtoList, movieIdPage.getPageable(), movieIdPage.getTotalElements());
  }

  private List<Movie> reorderByIdsOrdering(List<UUID> ids, List<Movie> movies) {
    var movieById = movies.stream()
        .collect(Collectors.toMap(Movie::getId, Function.identity()));

    return ids.stream()
        .map(movieById::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public void refreshMoviesExternalInfo() {
    movieRepo.findWithExternalAggregatorInfos()
        .forEach(this::refreshMovieExternalInfo);
  }

  private void refreshMovieExternalInfo(Movie movie) {
    var externalAggregatorInfos = movie.getExternalAggregatorInfoAsMap();
    var movieRequestDto = MovieRequestDto.builder()
        .imdbUrl(externalAggregatorInfos.get(IMDB).getUrl())
        .kinopoiskUrl(externalAggregatorInfos.get(KINOPOISK).getUrl())
        .build();

    try {
      populateOmdbData(movie, movieRequestDto);
      populateKinopoiskData(movie, movieRequestDto);
    } catch (Exception e) {
      log.error("Throw an exception during movie external ratings refresh", e);
    }
  }
}
