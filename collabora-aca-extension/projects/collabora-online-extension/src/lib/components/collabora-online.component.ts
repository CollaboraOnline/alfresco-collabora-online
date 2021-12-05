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
import { Component, OnInit, OnDestroy, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentApiService } from '@alfresco/aca-shared';
import { MinimalNodeEntryEntity } from '@alfresco/js-api';
import { CollaboraOnlineService } from '../services/collabora-online.service';

@Component({
  selector: 'collabora-online',
  templateUrl: './collabora-online.component.html',
  styleUrls: ['./collabora-online.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CollaboraOnlineComponent implements OnInit, OnDestroy {

  @ViewChild('form') postForm: ElementRef;
  @ViewChild('access_token') inputToken: ElementRef;
  @ViewChild('access_token_ttl') inputTokenTTL: ElementRef;
  @ViewChild('browserFrame') browserFrame: ElementRef;

  action: string;
  nodeId: string;
  node: MinimalNodeEntryEntity;
  fileName: string;
  mimeType: string;
  previousUrl: string;
  accessToken: string;
  accessTokenTTL: string;
  iFrameUrl: string;
  listenerHandlePostMessage: any;

  constructor(private collaboraOnlineService: CollaboraOnlineService,
              private route: ActivatedRoute,
              private contentApi: ContentApiService,
              private router: Router) {
    this.action = this.route.snapshot.params['action'];
    this.nodeId = this.route.snapshot.params['nodeId'];
  }

  async ngOnInit() {
    // Get the node
    this.node = await this.contentApi.getNodeInfo(this.nodeId).toPromise();
    if (this.node) {
      this.fileName = this.node.name;
      if (this.node.content) {
        this.mimeType = this.node.content.mimeType;
      }
    }

    // Get previous url
    this.previousUrl = this.collaboraOnlineService.getPreviousUrl();

    // Get url du serveur collabora online
    const wopiHostUrl =  await this.collaboraOnlineService.getLoolUrl();
    const wopiFileUrl = wopiHostUrl + 'wopi/files/' + this.nodeId;

    // Get token pour l'édition du document
    var responseToken: any = await this.collaboraOnlineService.getAccessToken(this.nodeId, this.action);
    this.accessToken = responseToken.access_token;
    this.accessTokenTTL = responseToken.access_token_ttl;
    if (!responseToken.wopi_src_url || responseToken.wopi_src_url == "") {
      responseToken = await this.collaboraOnlineService.getAccessToken(this.nodeId, 'edit');
    }
    const wopiSrcUrl = responseToken.wopi_src_url;
    this.iFrameUrl = wopiSrcUrl + 'WOPISrc=' + encodeURI(wopiFileUrl);
    if (this.action === 'view') {
      this.iFrameUrl += '&permission=readonly';
    }

    // Remplissage du formulaire dynamique
    this.postForm.nativeElement.action = this.iFrameUrl
    this.inputToken.nativeElement.value = this.accessToken;
    this.inputTokenTTL.nativeElement.value = this.accessTokenTTL;

    // Déclenchement du post
    this.postForm.nativeElement.submit();

    // Ajout de listener pour les évènement
    if (!this.listenerHandlePostMessage) {
        this.listenerHandlePostMessage = this.handlePostMessage.bind(this);
    }
    window.addEventListener("message", this.listenerHandlePostMessage, true);
  }

  ngOnDestroy(){
    window.removeEventListener("message", this.listenerHandlePostMessage, true);
  }

  private handlePostMessage(event: any): void {
    var message = JSON.parse(event.data);
    var id = message.MessageId;
    var values = message.Values;

    switch (id) {
      case 'UI_Close':
        console.log("PostMessage Recev: UI_CLose - move to");
        // Go back to previous page
        this.router.navigateByUrl(this.previousUrl);
        break;
      case 'App_LoadingStatus':
        if (values.Status === 'Frame_Ready') {
          // Add readonly
        }
        console.log("PostMessage Recev: App_LoadingStatus - Status:" + values.Status);
        break;
      case 'View_Added':
        console.log("PostMessage Recev: View_Added - Values:" + JSON.stringify(values));
        break;
      default:
        console.log("MessageID : " + id + " - Values : " + values);
    }
  }

  onBackButtonClick(): void {
    this.router.navigateByUrl(this.previousUrl);
  }

  onFullscreenClick(): void {
    const container = <any>(
      document.documentElement.querySelector(
        '.adf-viewer__fullscreen-container'
      )
    );
    if (container) {
      if (container.requestFullscreen) {
        container.requestFullscreen();
      } else if (container.webkitRequestFullscreen) {
        container.webkitRequestFullscreen();
      } else if (container.mozRequestFullScreen) {
        container.mozRequestFullScreen();
      } else if (container.msRequestFullscreen) {
        container.msRequestFullscreen();
      }
    }
  }
}
