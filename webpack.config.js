/* global module, require, __dirname */

const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');

module.exports = {
  entry: {
    public: './client/src-js/index.js',
    'wh-legacy': './client/legacy-styles/wh.sass',
    'wh-styles': './client/styles/index.js'
  },
  output: {
    path: path.resolve(__dirname, 'client/resources/public'),
    filename: '[name].js'
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: './[name].css'
    })
  ],
  mode: 'development',
  devtool: 'source-map',
  module: {
    rules: [
      {
        test: /\.sass$/,
        include: [path.resolve(__dirname, 'client/styles')],
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
            loader: path.resolve(__dirname, 'cljc-css-loader/index.js'),
            options: {
              path: path.resolve(__dirname, 'common/src/wh/styles'),
              nsPrefix: 'wh.styles'
            }
          },
          {
            loader: 'css-loader',
            options: {
              importLoaders: 1,
              modules: {
                mode: 'local',
                localIdentName: '[name]__[local]--[hash:base64:5]'
              }
            }
          },
          {
            loader: 'postcss-loader'
          },
          {
            loader: 'sass-loader'
          }
        ]
      },
      {
        test: /\.sass$/,
        include: [path.resolve(__dirname, 'client/legacy-styles')],
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
            loader: 'css-loader?-url'
          },
          {
            loader: 'postcss-loader'
          },
          {
            loader: 'sass-loader'
          }
        ]
      }
    ]
  }
};
