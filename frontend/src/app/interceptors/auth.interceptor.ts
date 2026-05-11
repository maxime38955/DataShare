import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Récupère le token depuis le localStorage (là où tu le stockes lors du login)
  const token = localStorage.getItem('token');

  // Si le token existe, on clone la requête pour ajouter le header
  if (token) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedRequest);
  }

  // Sinon, on laisse passer la requête telle quelle (pour le login/register par exemple)
  return next(req);
};