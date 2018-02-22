package kamilmilik.licencjat_gps_kid.models

/**
 * Created by kamil on 12.02.2018.
 */
//We frequently create classes whose main purpose is to hold data nd is marked as data(automatically equals,hashCode,toString etc)
class UserTestOnline {
    var email: String = ""
    var status: String = ""
    var userId : String? = null

    constructor() {}
    constructor(userId : String,email : String){
        this.userId = userId
        this.email = email
    }
    constructor(email: String, status: String, emptyString : String) {
        this.email = email
        this.status = status
    }
}