/*
 * Copyright (C) 2020 Jeci.
 * https://jeci.fr/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { Injectable } from '@angular/core';
import { MinimalNodeEntryEntity } from '@alfresco/js-api';
import { Router, NavigationEnd } from '@angular/router';
import { AlfrescoApiService } from '@alfresco/adf-core';

@Injectable({
  providedIn: 'root'
})
export class CollaboraOnlineService {

  private previousUrl: string;
  private currentUrl: string;
  isLoading = false;
  displayNode: MinimalNodeEntryEntity = null;

  constructor(private router: Router,
              private apiService: AlfrescoApiService) {
    this.currentUrl = this.router.url;
    this.router.events.subscribe(
      event => {
        if (event instanceof NavigationEnd) {
          this.previousUrl = this.currentUrl;
          this.currentUrl = event.url;
        }
      }
    );
  }

  onEdit(node: MinimalNodeEntryEntity) {
    if (node.isFile) {
      this.triggerEditCollaboraOnline(node);
    }
  }

  private triggerEditCollaboraOnline(node: MinimalNodeEntryEntity) {
    this.router.navigate(["/collabora-online-edit", node.id]);
  }

  public getLoolUrl() {
    return new Promise(
      (resolve, reject) => {
        this.apiService.getInstance().webScript.executeWebScript('GET', 'lool/host/url').then(
          (response) => {
            resolve(response.lool_host_url);
          },
          (error) => {
            reject(error);
          }
        );
      }
    );
  }

  public getAccessToken(nodeId: string) {
    return new Promise(
      (resolve, reject) => {
        this.apiService.getInstance().webScript.executeWebScript('GET', 'lool/token?nodeRef=workspace://SpacesStore/' + nodeId + '&action=edit').then(
          (response) => {
            resolve(response);
          },
          (error) => {
            reject(error);
          }
        );
      }
    );
  }

  public getPreviousUrl() {
    return this.previousUrl;
  }

  public onDownload(url: string, fileName: string) {
    if (url && fileName) {
      const link = document.createElement('a');

      link.style.display = 'none';
      link.download = fileName;
      link.href = url;

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }

}
