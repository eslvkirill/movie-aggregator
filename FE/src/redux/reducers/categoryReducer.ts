import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import {
	addCategoryAction,
	onChangeEventAction,
	editCategoryAction,
	deleteCategoryAction,
} from 'redux/actions/categoryAction';
import {
	getCategoriesCreator,
	getCategoryMoviesCreator,
} from 'redux/creators/categoryCreator';
import { REDUCER } from '../types/reducers';

const initialState: any = {
	categories: [],
	categoryList: [],
	movies: [],
	isLoading: true,
	error: '',
};

const categoryReducer = createSlice({
	name: REDUCER.CATEGORY,
	initialState,
	reducers: {
		addCategory: addCategoryAction,
		onChangeEvent: onChangeEventAction,
		editCategory: editCategoryAction,
		deleteCategory: deleteCategoryAction,
		resetMovies: (state) => {
			state.movies = [];
		},
		resetCalegoriesList: (state) => {
			state.categoryList = [];
		},
	},
	extraReducers: (builder) => {
		builder
			.addCase(
				getCategoriesCreator.fulfilled.type,
				(state, action: PayloadAction<any>) => {
					const categories = action.payload;
					categories.map((category: any) => (category.open = false));
					state.categoryList = [];

					state.isLoading = false;
					state.categories = categories;

					Object.keys(action.payload).map((category) => {
						state.categoryList.push({
							value: action.payload[category].id,
							label: action.payload[category].name,
						});

						return state.categoryList;
					});
				}
			)
			.addCase(getCategoriesCreator.pending.type, (state) => {
				state.isLoading = true;
			})
			.addCase(
				getCategoriesCreator.rejected.type,
				(state, action: PayloadAction<string>) => {
					state.isLoading = true;
					state.error = action.payload;
				}
			)
			.addCase(
				getCategoryMoviesCreator.fulfilled.type,
				(state, action: PayloadAction<any>) => {
					action.payload.map((movie: any) =>
						Object.values(movie).map((item) => state.movies.push(item))
					);
				}
			);
	},
});

export const {
	addCategory,
	onChangeEvent,
	editCategory,
	deleteCategory,
	resetMovies,
	resetCalegoriesList,
} = categoryReducer.actions;

export default categoryReducer.reducer;
