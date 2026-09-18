import type {AuthModels_UserInfo} from './';

export interface AuthModels_GetInfoResponse {
    readonly user: AuthModels_UserInfo;
    readonly roles: ReadonlyArray<string>;
    readonly perms: ReadonlyArray<string>;
}
