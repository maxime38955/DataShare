import { Component, signal } from '@angular/core';
import { RouterOutlet, ɵEmptyOutletComponent } from '@angular/router';
import { UserService } from './services/user.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ɵEmptyOutletComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');

 

}
