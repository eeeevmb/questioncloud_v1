import { defineStore } from 'pinia';
import type { UserBasicInfo } from '../types/user';
import { fetchBasicInfo } from '../api/user';

interface State {
  user: UserBasicInfo | null;
  initialized: boolean;
  loading: boolean;
  avatarVersion: number;
}

export const useAuthStore = defineStore('auth', {
  state: (): State => ({
    user: null,
    initialized: false,
    loading: false,
    avatarVersion: Date.now()
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.user)
  },
  actions: {
    async initialize() {
      if (this.initialized) {
        return;
      }
      this.initialized = true;
      await this.refreshUser();
    },
    async refreshUser() {
      this.loading = true;
      try {
        const profile = await fetchBasicInfo();
        this.user = profile;
      } catch (error) {
        this.user = null;
        throw error;
      } finally {
        this.loading = false;
      }
    },
    setUser(user: UserBasicInfo | null) {
      this.user = user;
    },
    bumpAvatarVersion() {
      this.avatarVersion = Date.now();
    },
    clear() {
      this.user = null;
      this.initialized = false;
      this.avatarVersion = Date.now();
    }
  }
});
