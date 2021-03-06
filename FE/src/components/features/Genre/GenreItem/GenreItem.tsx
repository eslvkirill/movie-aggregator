import { CSSTransition } from 'react-transition-group';
import Button from 'components/shared/form-controls/Button/Button';
import Input from 'components/shared/form-controls/Input/Input';
import { Genre, GenresProperties } from '../genre.interface';
import './GenreItem.scss';

const GenreItem = (props: GenresProperties) => {
  const { dropdown, genres, updateGenre, saveAction, editAction, disabled, deleteGenre } = props;

  return (
    <CSSTransition 
      in={dropdown}
      appear
      exit
      unmountOnExit
      classNames="fade"
      timeout={{
        enter: 300,
        exit: 200,
      }}
    >
      <ul className="genre-item">
        {genres.map((genre: Genre) => {
          const { id, name, open } = genre; 

          return (
            <li key={id}>
              <Input
                id={id}
                value={name}
                onChange={(event) => updateGenre(event, id)}
                disabled={!open}
                // autocomplete="off"
              />
              <Button
                type="submit"
                id={id}
                onClick={() => open ? saveAction(id) : editAction(id)}
                disabled={disabled}
              >
                {open ? 'Сохранить' : 'Редактировать'}
              </Button>
              <Button type="submit" onClick={() => deleteGenre(id)}>
                Удалить
              </Button>
            </li>
          );
        })}
      </ul>
    </CSSTransition>
  )
};

export default GenreItem;