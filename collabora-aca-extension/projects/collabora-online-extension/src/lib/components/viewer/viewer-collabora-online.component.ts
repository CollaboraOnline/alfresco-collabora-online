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
import { Component, Input, OnInit, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';
import { CollaboraOnlineService } from '../../services/collabora-online.service';

@Component({
  selector: 'viewer-collabora-online',
  templateUrl: './viewer-collabora-online.component.html',
  styleUrls: ['./viewer-collabora-online.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ViewerCollaboraOnlineComponent implements OnInit {

  @ViewChild('form') postForm: ElementRef;
  @ViewChild('access_token') inputToken: ElementRef;
  @ViewChild('access_token_ttl') inputTokenTTL: ElementRef;
  @ViewChild('browserFrame') browserFrame: ElementRef;

  @Input()
  nodeId: string;

  accessToken: string;
  accessTokenTTL: String;
  iFrameUrl: string;


  constructor(private collaboraOnlineService: CollaboraOnlineService) {
  }

  async ngOnInit() {
    console.log("Node id : " + this.nodeId);
    // Get url du serveur collabora online
    const wopiHostUrl =  await this.collaboraOnlineService.getLoolUrl();
    const wopiFileUrl = wopiHostUrl + 'wopi/files/' + this.nodeId;

    // Get token pour l'édition du document
    var responseToken: any = await this.collaboraOnlineService.getAccessToken(this.nodeId, 'view');
    this.accessToken = responseToken.access_token;
    this.accessTokenTTL = responseToken.access_token_ttl;
    if (!responseToken.wopi_src_url || responseToken.wopi_src_url == "") {
      responseToken = await this.collaboraOnlineService.getAccessToken(this.nodeId, 'edit');
    }
    const wopiSrcUrl = responseToken.wopi_src_url;
    this.iFrameUrl = wopiSrcUrl + 'WOPISrc=' + encodeURI(wopiFileUrl) + '&permission=readonly';

    // Remplissage du formulaire dynamique
    this.postForm.nativeElement.action = this.iFrameUrl
    this.inputToken.nativeElement.value = this.accessToken;
    this.inputTokenTTL.nativeElement.value = this.accessTokenTTL;

    // Déclenchement du post
    this.postForm.nativeElement.submit();
  }
}
