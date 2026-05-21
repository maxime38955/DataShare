import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FileService } from './file.service';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('FileService', () => {
  let service: FileService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api/v1/files';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FileService]
    });
    service = TestBed.inject(FileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait appeler getUserFiles et retourner des données', () => {
    const mockFiles = [{ fileId: 1, name: 'test.pdf' }];

    service.getUserFiles().subscribe(files => {
      expect(files).toEqual(mockFiles);
    });

    const req = httpMock.expectOne(`${apiUrl}/user/files`);
    expect(req.request.method).toBe('GET');
    req.flush(mockFiles);
  });

  it('devrait supprimer un fichier via DELETE', () => {
    service.deleteFile(1).subscribe(res => {
      expect(res).toBe('Success');
    });

    const req = httpMock.expectOne(`${apiUrl}/user/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush('Success');
  });
});