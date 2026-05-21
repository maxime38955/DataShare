import { Routes } from '@angular/router';
import { UploadComponent } from './components/upload/upload';
import { HomeComponent } from './components/home/home';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { DownloadComponent } from './components/download/download';
import { ProfilComponent } from './components/profil/profil';

export const routes: Routes = [

     {path: 'upload', component: UploadComponent},
     {path: 'home', component: HomeComponent},
     {path: '', component: HomeComponent},
     {path: 'login', component: LoginComponent},
     {path: 'register', component: RegisterComponent},
     {path: 'download', component: DownloadComponent},
     {path: 'profil', component: ProfilComponent},
     { path: 'download/:token', component: DownloadComponent }

];
