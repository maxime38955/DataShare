import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl = 'http://localhost:8080/api/v1/files/upload';

  constructor(private http: HttpClient) {}

  uploadFile(file: File, password?: string, expirationDays?: number, tags?: string[]): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    
    if (password) {
      formData.append('password', password);
    }
    
    if (expirationDays) {
      formData.append('expirationDays', expirationDays.toString());
    }
    
    if (tags && tags.length > 0) {
      // Spring Boot comprend parfaitement quand on ajoute plusieurs fois la même clé pour créer une liste/Set
      tags.forEach(tag => formData.append('tags', tag));
    }

    return this.http.post(this.apiUrl, formData);
  }
}