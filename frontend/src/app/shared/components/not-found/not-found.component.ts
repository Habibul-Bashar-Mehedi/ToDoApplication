import { Component } from '@angular/core';

@Component({
  selector: 'app-not-found',
  template: `
    <div style="text-align:center; padding: 4rem 2rem;">
      <h1>404 — Page Not Found</h1>
      <p>The page you are looking for does not exist.</p>
      <a href="/dashboard">Go to Dashboard</a>
    </div>
  `,
})
export class NotFoundComponent {}
