import { NavLink, Outlet } from 'react-router-dom';
import { useAppSelector } from '../../hooks/redux';
import { ROUTE } from '../../shared/constants/routes';
import './AdminPage.scss';

const AdminPage = () => {
  const isMovieEdit = useAppSelector(state => state.movieReducer.isEdit);
  const isPersonEdit = useAppSelector(state => state.personReducer.isEdit);

  const links = [
    { to: ROUTE.GENRES, label: 'Создание жанров' },
    { to: ROUTE.PERSONS, label: `${isPersonEdit ? 'Редактирование актёра / режиссёра'  : 'Создание актёров / режиссёров'}` },
    { to: ROUTE.MOVIES, label: `${isMovieEdit ? 'Редактирование фильма'  : 'Создание фильмов'}` },
    { to: ROUTE.ROLES, label: 'Редактирование ролей' },
  ];

  return (
    <div className="admin-page">
      <ul className="admin-page__menu">
        {links.map((link) => (
          <li key={link.to}>
            <NavLink 
              className={({ isActive }) => isActive ? 'active' : 'link'} 
              to={link.to}
            >
              {link.label}
            </NavLink>
          </li>
        ))}
      </ul>
      
      <Outlet/>
    </div>
  );
};

export default AdminPage;
