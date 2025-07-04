<!--
  Copyright (C) 2022 - Jeci SARL - https://jeci.fr

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see https://www.gnu.org/licenses/agpl-3.0.html.
-->

<template>
  <h1>Example Collabora with Vue</h1>
  <collabora-iframe
    v-if="wopiFileUrl && collaboraUrl"
    :access-token="accessToken"
    :access-token-t-t-l="accessTokenTTL"
    :collabora-url="collaboraUrl"
    :wopi-file-url="wopiFileUrl"
  ></collabora-iframe>
</template>
<script>
import CollaboraIframe from "./components/CollaboraIframe.vue";
import axios from "axios";

export default {
  name: "CollaboraOnlineVue",
  components: {
    CollaboraIframe,
  },
  data() {
    return {
      accessToken: null,
      accessTokenTTL: null,
      collaboraUrl: null,
      wopiFileUrl: null,
    };
  },
  created() {
    this.loadExample();
  },
  methods: {
    loadExample() {
      // This node needs to be changed to an existing node on your server
      let nodeId = "0dde2d84-0ba6-4f6e-9f0a-0eb9f15d9888";
      this.getLoolUrl()
        .then((loolUrl) => {
          this.wopiFileUrl = encodeURI(
            `${loolUrl["lool_host_url"]}wopi/files/${nodeId}`,
          );
          return this.getAccessToken(nodeId, "edit");
        })
        .then((getAccessToken) => {
          this.accessToken = getAccessToken["access_token"];
          this.accessTokenTTL = getAccessToken["access_token_ttl"];
          this.collaboraUrl = getAccessToken["wopi_src_url"];
        });
    },
    getLoolUrl() {
      // local path is proxied by Vite from Alfresco Server
      let path = `http://localhost:8008/alfresco/service/lool/host/url`;
      return axios
        .get(path)
        .then((resp) => {
          return resp.data;
        })
        .catch((error) => {
          console.log(`erreur : ${error}`);
          return error;
        });
    },
    getAccessToken(nodeId, action) {
      let path = `http://localhost:8008/alfresco/service/lool/token?nodeRef=workspace://SpacesStore/${nodeId}&action=${action}`;
      return axios
        .get(path)
        .then((resp) => {
          return resp.data;
        })
        .catch((error) => {
          console.log(`erreur : ${error}`);
          return error;
        });
    },
  },
};
</script>

<style></style>
