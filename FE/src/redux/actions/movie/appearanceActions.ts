import { MovieFormFileds } from 'components/features/Movie/movie.enum';

const setAggregatorsRatingsOnGeneralViewAction = (aggregators: any) => {
	aggregators.map((aggregator: any) => {
		const name = aggregator.url.split('.')[1];

		aggregator.name = name.charAt(0).toUpperCase() + name.slice(1);

		if (aggregator.name === 'Metacritic') {
			aggregator.rating /= 10;
		}

		return aggregator;
	});
};

const sortAggregatorsAction = (aggregators: any) => {
	aggregators.sort((present: any, next: any) =>
		present.name > next.name ? 1 : -1
	);

	[aggregators[0], aggregators[1]] = [aggregators[1], aggregators[0]];
};

const setDefaultFieldsAppearanceAction = (movie: any) => {
	const personFieldNames: string[] = [
		MovieFormFileds.GENRES,
		MovieFormFileds.ACTORS,
		MovieFormFileds.DIRECTORS,
	];

	const exludedFields: string[] = [
		MovieFormFileds.AVERAGE_RATINGS,
		MovieFormFileds.USER_RATINGS,
		MovieFormFileds.EXTERNAL_AGGREGATORS_INFO,
	];

	Object.keys(movie).map((name) => {
		if (Array.isArray(movie[name]) && !exludedFields.includes(name)) {
			const movieFieldValue = movie[name].map((field: any) => field);

			movie[name] = personFieldNames.includes(name)
				? movieFieldValue
				: movieFieldValue.join(', ');

			movie.displayGenres = movie[MovieFormFileds.GENRES]
				.map((field: any) => field.name)
				.join(', ');
		} else if (name === MovieFormFileds.DURATION) {
			const durationParts = movie[name].split(':');

			movie[name] = `${durationParts[0]}ч ${durationParts[1]}м`;
		}
		return movie[name];
	});
};

const setBackgroundAction = (movieBackground: string) => {
	let style = {};

	if (movieBackground) {
		style = {
			backgroundImage: `linear-gradient(rgba(250, 250, 250, 0.1), rgba(0, 0, 0, 0.7)), 
        url("data:image/*;base64,${movieBackground}")`,
			backgroundSize: 'cover',
			backgroundPosition: 'center center',
			backgroundRepeat: 'no-repeat',
		};
	}
	return style;
};

export {
	setAggregatorsRatingsOnGeneralViewAction,
	sortAggregatorsAction,
	setDefaultFieldsAppearanceAction,
	setBackgroundAction,
};
