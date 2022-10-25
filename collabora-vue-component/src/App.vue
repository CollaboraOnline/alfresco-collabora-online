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
