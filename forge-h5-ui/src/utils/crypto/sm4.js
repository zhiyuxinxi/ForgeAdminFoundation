import { sm4 } from 'sm-crypto'

export function sm4Encrypt(plainText, key) {
  if (!plainText) {
    return plainText
  }

  return hexToBase64(sm4.encrypt(plainText, key))
}

export function sm4Decrypt(cipherText, key) {
  if (!cipherText) {
    return cipherText
  }

  return sm4.decrypt(base64ToHex(cipherText), key)
}

function hexToBase64(hexString) {
  const bytes = []
  for (let i = 0; i < hexString.length; i += 2) {
    bytes.push(Number.parseInt(hexString.substr(i, 2), 16))
  }
  return btoa(String.fromCharCode.apply(null, bytes))
}

function base64ToHex(base64String) {
  const raw = atob(base64String)
  let result = ''
  for (let i = 0; i < raw.length; i++) {
    const hex = raw.charCodeAt(i).toString(16)
    result += hex.length === 2 ? hex : `0${hex}`
  }
  return result
}

export function decodeKeyToHex(base64Key) {
  return base64ToHex(base64Key)
}
