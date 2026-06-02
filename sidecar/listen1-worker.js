const api = require('./listen1-api.min.js');
api.loadNodejsDefaults();

process.on('unhandledRejection', (reason) => {
    console.log(JSON.stringify({ code: 500, message: String(reason) }));
    process.exit(1);
});
process.on('uncaughtException', (err) => {
    console.log(JSON.stringify({ code: 500, message: err.message }));
    process.exit(1);
});

const args = process.argv.slice(2);
if (args.length < 1) {
    console.error('Usage: node listen1-worker.js <apiName> [params...]');
    process.exit(1);
}

const apiName = args[0];
const params = args.slice(1).join('&');

api.apiGet('/' + apiName + (params ? '?' + params : ''))
    .then(data => {
        console.log(JSON.stringify({ code: 200, data }));
        process.exit(0);
    })
    .catch(err => {
        console.log(JSON.stringify({ code: 500, message: err.message }));
        process.exit(1);
    });

setTimeout(() => {
    console.log(JSON.stringify({ code: 500, message: 'timeout' }));
    process.exit(1);
}, 30000);
