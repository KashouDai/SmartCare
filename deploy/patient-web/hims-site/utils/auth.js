import Cookies from 'js-cookie'

export function getCodeImg() {
}


const TokenKey = 'Patient-Token'
const userId = "Patient-User-Id";
const name = "Patient-Name";

export function getToken() {
  return Cookies.get(TokenKey)
}

export function getParam(param) {
  if (param === 'userId') {
    return Cookies.get(userId) || Cookies.get(param)
  }
  return Cookies.get(param)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function setOther(response) {

  Cookies.set(userId, response.userid)
  return Cookies.set(name, response.sub)
}

export function removeToken() {
  Cookies.remove(userId)
  Cookies.remove(name)
  return Cookies.remove(TokenKey)
}
