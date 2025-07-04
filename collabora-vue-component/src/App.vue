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
    this.loadFromParams();
  },
  methods: {
    loadFromParams() {
      const queryString = window.location.search;
      const urlParams = new URLSearchParams(queryString);
      this.accessToken = urlParams.get("accessToken");
      this.accessTokenTTL = urlParams.get("accessTokenTTL");
      this.wopiFileUrl = urlParams.get("wopiFileUrl");
      this.collaboraUrl = urlParams.get("collaboraUrl");
    },
  },
};
</script>

<style></style>
