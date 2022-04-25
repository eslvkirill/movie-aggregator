import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Genre } from 'components/feature/Genre/genre.interface';
import { GenreState } from 'redux/types/genre';
import {
	addGenreAction,
	onChangeEventAction,
	editGenreAction,
	deleteGenreAction,
} from 'redux/actions/genreActions';
import { getGenresCreator } from 'redux/actions/creators/genreCreator';

const initialState: GenreState = {
	genres: [],
	isLoading: true,
	error: '',
};

const genreReducer = createSlice({
	name: 'genre',
	initialState,
	reducers: {
		addGenre: addGenreAction,
		onChangeEvent: onChangeEventAction,
		editGenre: editGenreAction,
		deleteGenre: deleteGenreAction,
	},
	extraReducers: (builder) => {
		builder
			.addCase(
				getGenresCreator.fulfilled.type,
				(state, action: PayloadAction<Genre[]>) => {
					const genres = action.payload;
					genres.map((genre: Genre) => (genre.open = false));

					state.isLoading = false;
					state.genres = genres;
				}
			)
			.addCase(getGenresCreator.pending.type, (state: GenreState) => {
				state.isLoading = true;
			})
			.addCase(
				getGenresCreator.rejected.type,
				(state, action: PayloadAction<string>) => {
					state.isLoading = true;
					state.error = action.payload;
				}
			);
	},
});

export const { addGenre, onChangeEvent, editGenre, deleteGenre } =
	genreReducer.actions;

export default genreReducer.reducer;