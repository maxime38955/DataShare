describe('Flux d\'authentification (Mocké)', () => {

  // Avant chaque test, on repart d'une page blanche
  beforeEach(() => {
    cy.visit('http://localhost:4200/');
  });

  describe('Flux Accueil (Home)', () => {
    
    it('doit afficher les boutons publics si non connecté', () => {
      cy.visit('http://localhost:4200/');
      
      // Vérification du texte principal
      cy.get('.title').should('contain', 'DataShare');
      cy.get('.subtitle').should('contain', 'Nous gardons vos fichiers');

      // Les boutons Login et Register doivent être là
      cy.get('.button-group').contains('Login').should('be.visible');
      cy.get('.button-group').contains('Register').should('be.visible');
      
      // Le bouton "Mon Profil" ne doit PAS être là
      cy.get('.button-group').contains('Mon Profil').should('not.exist');
    });

    it('doit afficher les boutons privés si connecté', () => {
      // MOCK : On simule la présence d'un token pour que le UserService croie qu'on est connecté
      cy.window().then((win) => {
        win.localStorage.setItem('auth_token', 'faux.token.jwt');
      });

      cy.visit('http://localhost:4200/');

      // Le bouton "Mon Profil" doit apparaître
      cy.get('.button-group').contains('Mon Profil').should('be.visible');
      
      // Le bouton de déconnexion doit être là
      cy.get('.btn-logout').should('be.visible');

      // Les boutons Login/Register doivent avoir disparu
      cy.get('.button-group').contains('Login').should('not.exist');
    });
  });

  describe('Flux Upload et Download', () => {

    describe('Upload', () => {
      it('doit permettre d\'uploader un fichier avec des options', () => {
        cy.visit('http://localhost:4200/upload');
        
        // 1. Simuler l'ajout d'un fichier dans le <input type="file"> caché
        cy.get('input[type="file"]').selectFile('cypress/fixtures/test-file.txt', { force: true });
        
        // Vérification : l'interface doit montrer le nom du fichier
        cy.get('.file-info').should('contain', 'test-file.txt');
        
        // 2. Remplir le formulaire
        cy.get('input[formControlName="password"]').type('Secret123', { force: true });
        
        // Ajouter un tag
        cy.get('input[formControlName="tagInput"]').type('travail{enter}', { force: true });
        cy.get('mat-chip-row').should('contain', 'travail');
        
        // 3. MOCK l'envoi
        cy.intercept('POST', '**/upload', {
          statusCode: 200,
          body: { token: 'super-token-123' }
        }).as('uploadRequest');
        
        cy.get('button[type="submit"]').click();
        cy.wait('@uploadRequest');
      });
    });

    describe('Download', () => {
    
    beforeEach(() => {
      // Les métadonnées du fichier
      cy.intercept('GET', '**/files/metadata/mon-token-secret', {
        statusCode: 200,
        body: { name: 'secret.pdf', size: 1048576, expirationDate: '2099-01-01', password: true }
      }).as('getMetadata');
    });

    it('doit demander le mot de passe, télécharger le fichier et afficher le succès', () => {
      // CORRECTION ICI : On intercepte un GET vers /files/download/mon-token-secret*
      cy.intercept('GET', '**/files/download/mon-token-secret*', {
        statusCode: 200,
        body: new Blob(['contenu du fichier fictif'], { type: 'text/plain' })
      }).as('downloadSuccess');

      cy.visit('http://localhost:4200/download/mon-token-secret');
      cy.wait('@getMetadata');

      // On tape le bon mot de passe
      cy.get('input#password').type('MotDePasse123');
      cy.get('.btn-download').should('not.be.disabled').click();
      
      // On attend la fausse requête de succès
      cy.wait('@downloadSuccess');

      // Vérifications du succès
      cy.get('.download-form').should('not.exist');
      cy.get('.success-state').should('be.visible').and('contain', 'Fichier téléchargé avec succès');
    });

    it('doit afficher une erreur si le mot de passe est incorrect', () => {
      // CORRECTION ICI AUSSI : On intercepte un GET qui va simuler une erreur 403
      cy.intercept('GET', '**/files/download/mon-token-secret*', {
        statusCode: 403,
        body: { error: 'Mauvais mot de passe' }
      }).as('downloadError');

      cy.visit('http://localhost:4200/download/mon-token-secret');
      cy.wait('@getMetadata');

      // On tape un mauvais mot de passe
      cy.get('input#password').type('FauxMotDePasse');
      cy.get('.btn-download').should('not.be.disabled').click();
      
      // On attend la fausse requête d'erreur
      cy.wait('@downloadError');

      // Vérifications de l'erreur
      // Le formulaire doit toujours être là
      cy.get('.download-form').should('be.visible');
      // Le message d'erreur rouge doit apparaître
      cy.get('.error-message').should('be.visible').and('contain', 'Mauvais mot de passe.');
    });

  });
  });

  describe('Inscription', () => {
    
    it('doit afficher une erreur si les mots de passe ne correspondent pas', () => {
      cy.get('button.btn-primary').contains('Register').click();
      
      // Actions utilisateur
      cy.get('[name="email"]').type('test@mail.com');
      cy.get('[name="password"]').type('MotDePasse123');
      
      // NOUVEAU : On tape une confirmation différente
      cy.get('[name="passwordConf"]').type('MotDePasseDIFFERENT');
      
      // NOUVEAU : On clique ailleurs pour déclencher le "touched"
      cy.get('.auth-title').click();
      
      // Vérification : le message d'erreur de correspondance s'affiche
      cy.get('.field-error').should('contain', 'Les mots de passe ne correspondent pas.');
      
      // Le bouton doit être désactivé
      cy.get('button.btn-submit').should('be.disabled');
    });

    it('doit simuler une inscription réussie', () => {
      cy.intercept('POST', '**/register', { 
        statusCode: 201, 
        body: { message: "Utilisateur créé avec succès" } 
      }).as('registerRequest');

      cy.get('button.btn-primary').contains('Register').click();

      cy.get('[name="email"]').type('nouveau@test.com');
      cy.get('[name="password"]').type('MotDePasseSecret123');
      
      // NOUVEAU : On valide la confirmation avec le MEME mot de passe
      cy.get('[name="passwordConf"]').type('MotDePasseSecret123'); 
      
      // Le bouton est actif, on soumet
      cy.get('button.btn-submit').should('not.be.disabled').click();

      cy.wait('@registerRequest'); 
    });
  });

  describe('Connexion', () => {

    it('doit connecter l\'utilisateur et le rediriger vers son profil', () => {
      cy.intercept('POST', '**/login', {
        statusCode: 200,
        body: { token: 'faux.token.jwt' }
      }).as('loginRequest');

      cy.get('button.btn-outline').contains('Login').click();
      
      cy.url().should('include', '/login');

      cy.get('[name="email"]').type('test@test.com');
      cy.get('[name="password"]').type('password123');
      cy.get('button.btn-submit').click();

      cy.wait('@loginRequest');
      
      cy.url().should('include', '/profil');
    });

    it('doit afficher une erreur en cas de mauvais mot de passe', () => {
      cy.intercept('POST', '**/login', {
        statusCode: 401,
        body: { error: 'Unauthorized' }
      }).as('loginFailed');

      cy.visit('http://localhost:4200/login'); 

      cy.get('[name="email"]').type('test@test.com');
      cy.get('[name="password"]').type('mauvaisMotDePasse');
      cy.get('button.btn-submit').click();

      cy.wait('@loginFailed');

      cy.get('.error-message')
        .should('be.visible')
        .and('contain', 'Identifiants incorrects');
    });
  });
});

describe('Flux Profil (Tableau de bord)', () => {

  beforeEach(() => {
    cy.window().then((win) => {
      win.localStorage.setItem('auth_token', 'header.eyJzdWIiOiJ0ZXN0QG1haWwuY29tIn0.signature');
    });

    cy.intercept('GET', '**/files/user/files', {
      statusCode: 200,
      body: [
        { fileId: 1, name: 'document.pdf', expirationDate: '2099-12-31T00:00:00Z', password: true, active: true, token: 'abc1' },
        { fileId: 2, name: 'vieille_image.jpg', expirationDate: '2020-01-01T00:00:00Z', password: false, active: false, token: 'abc2' }
      ]
    }).as('getFiles');

    cy.visit('http://localhost:4200/profil');
    cy.wait('@getFiles');
  });

  it('doit afficher la liste des fichiers', () => {
    cy.get('.profile-name').should('contain', 'test@mail.com');
    cy.get('.file-item').should('have.length', 2);
    cy.get('.file-item').first().find('.lock-icon').should('be.visible');
  });

  it('doit filtrer les fichiers actifs et expirés', () => {
    cy.get('.tab').contains('Actifs').click();
    cy.get('.file-item').should('have.length', 1);
    cy.get('.file-item').contains('document.pdf').should('be.visible');

    cy.get('.tab').contains('Expiré').click();
    cy.get('.file-item').should('have.length', 1);
    cy.get('.file-item').contains('vieille_image.jpg').should('be.visible');
  });

  it('doit faire expirer le fichier au clic sur supprimer', () => {
    cy.intercept('DELETE', '**/files/user/1', { statusCode: 200 }).as('deleteFile');
    
    cy.intercept('GET', '**/files/user/files', {
      statusCode: 200,
      body: [
        { fileId: 1, name: 'document.pdf', expirationDate: '2020-01-01T00:00:00Z', password: true, active: false, token: 'abc1' },
        { fileId: 2, name: 'vieille_image.jpg', expirationDate: '2020-01-01T00:00:00Z', password: false, active: false, token: 'abc2' }
      ]
    }).as('getFilesAfterDelete');

    cy.on('window:confirm', () => true);

    cy.get('.btn-delete').first().click();
    
    cy.wait('@deleteFile');
    cy.wait('@getFilesAfterDelete');
    
    cy.get('.file-item').should('have.length', 2);
    cy.get('.btn-delete').should('not.exist');
    cy.get('.expired-text')
      .should('have.length', 2)
      .and('contain', "Ce fichier a expiré, il n'est plus stocké chez nous");
  });
});