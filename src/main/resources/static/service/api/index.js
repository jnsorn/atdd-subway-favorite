const METHOD = {
    GET_WITH_AUTH(token) {
        return {
            method: 'GET',
            headers: {
                'Authorization': token,
            }
        }
    },
    PUT() {
        return {
            method: 'PUT'
        }
    },
    DELETE() {
        return {
            method: 'DELETE'
        }
    },
    POST(data) {
        return {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                ...data
            })
        }
    }
}

const api = (() => {
    const request = (uri, config) => fetch(uri, config)
    const requestWithJsonData = (uri, config) => fetch(uri, config).then(data => data.json())

    const line = {
        getAll() {
            return request(`/lines/detail`)
        },
        getAllDetail() {
            return requestWithJsonData(`/lines/detail`)
        }
    }

    const path = {
        find(params) {
            return requestWithJsonData(`/paths?source=${params.source}&target=${params.target}&type=${params.type}`)
        }
    }

    const member = {
        create(data) {
            return request(`/members`, METHOD.POST(data))
        },
        login(data) {
            return request(`/login`, METHOD.POST(data))
        },
        getWithAuth(token) {
            return requestWithJsonData(`/members`, METHOD.GET_WITH_AUTH(token))
        }
    }

    return {
        line,
        path,
        member
    }
})()

export default api
